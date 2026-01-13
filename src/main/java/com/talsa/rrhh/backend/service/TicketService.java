package com.talsa.rrhh.backend.service;

import com.talsa.rrhh.backend.dto.*;
import com.talsa.rrhh.backend.entity.*;
import com.talsa.rrhh.backend.enums.*;
import com.talsa.rrhh.backend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ==========================================
    // MÉTODOS AUXILIARES (CONVERSORES Y NOTIFICACIONES)
    // ==========================================

    /**
     * Convierte una Entidad Ticket a un DTO limpio para el Frontend.
     * Oculta datos sensibles del usuario y formatea el nombre del responsable.
     */
    private TicketResponseDTO convertirADTO(Ticket ticket) {
        // 1. Formatear trabajador de atención (RRHH) - EXISTENTE
        String nombreTrabajadorRRHH = null;
        if (ticket.getUsuarioAtencion() != null) {
            nombreTrabajadorRRHH = ticket.getUsuarioAtencion().getNombre() + " " +
                    ticket.getUsuarioAtencion().getApellidos();
        }

        // 2. BUSCAR NOMBRE DEL SOLICITANTE (TRABAJADOR) - NUEVO
        String nombreSolicitante = "DNI: " + ticket.getDniSolicitante(); // Valor por defecto
        var usuarioOpt = usuarioRepository.findByUsername(ticket.getDniSolicitante());

        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();
            nombreSolicitante = u.getNombre() + " " + u.getApellidos();
        }

        return TicketResponseDTO.builder()
                .id(ticket.getId())
                .dniSolicitante(ticket.getDniSolicitante())
                .nombreSolicitante(nombreSolicitante) // <--- ASIGNAMOS AQUÍ
                .lugarAtencion(ticket.getLugarAtencion())
                .categoria(ticket.getCategoria())
                .subCategoria(ticket.getSubCategoria())
                .observacion(ticket.getObservacion())
                .estado(ticket.getEstado())
                .trabajadorAtencion(nombreTrabajadorRRHH)
                .fechaCreacion(ticket.getFechaCreacion())
                .fechaInicioAtencion(ticket.getFechaInicioAtencion())
                .fechaFinAtencion(ticket.getFechaFinAtencion())
                .build();
    }

    /**
     * Notifica a la TV vía WebSocket.
     * Envía listas limpias (DTOs) para no exponer datos innecesarios en la pantalla pública.
     */
    private void notificarCambiosEnCola() {
        // 1. Canal de Espera
        List<TicketResponseDTO> pendientes = ticketRepository.findByEstado(EstadoTicket.PENDIENTE)
                .stream().map(this::convertirADTO).collect(Collectors.toList());
        messagingTemplate.convertAndSend("/topic/pendientes", pendientes);

        // 2. Canal de "Llamando Ahora"
        List<TicketResponseDTO> enAtencion = ticketRepository.findByEstado(EstadoTicket.EN_ATENCION)
                .stream().map(this::convertirADTO).collect(Collectors.toList());
        messagingTemplate.convertAndSend("/topic/en-atencion", enAtencion);

        System.out.println("📡 WebSocket: Listas actualizadas enviadas a la TV");
    }

    // ==========================================
    // 1. LÓGICA PARA LA TABLET (TRABAJADOR)
    // ==========================================

    @Transactional
    public TicketResponseDTO registrarSolicitud(TicketRegistroDTO dto) {

        // 1. Validar Usuario (Igual que antes)
        Usuario usuarioSolicitante = usuarioRepository.findByUsername(dto.getDniSolicitante())
                .orElseThrow(() -> new EntityNotFoundException("El DNI ingresado no corresponde a un usuario registrado."));

        // 2. Regla de Roles (Igual que antes)
        if (usuarioSolicitante.getRol() == Rol.JEFE_RRHH || usuarioSolicitante.getRol() == Rol.PERSONAL_RRHH) {
            throw new IllegalArgumentException("Acceso denegado: RRHH no puede crear tickets.");
        }

        // 3. REGLA ANTI-SPAM MEJORADA (CORRECCIÓN)
        // Buscamos si tiene ticket esperando (PENDIENTE) O siendo atendido (EN_ATENCION)
        boolean tieneTicketActivo = ticketRepository.existsByDniSolicitanteAndEstadoIn(
                usuarioSolicitante.getUsername(),
                List.of(EstadoTicket.PENDIENTE, EstadoTicket.EN_ATENCION)
        );

        if (tieneTicketActivo) {
            // Mensaje más claro para el usuario
            throw new IllegalStateException("Ya tienes un proceso en curso (En cola o siendo atendido). Termina el actual antes de solicitar otro.");
        }

        // 4. Creación (Igual que antes)
        Ticket nuevoTicket = Ticket.builder()
                .dniSolicitante(usuarioSolicitante.getUsername())
                .lugarAtencion(dto.getLugarAtencion())
                .categoria(dto.getCategoria())
                .observacion(dto.getObservacion())
                .estado(EstadoTicket.PENDIENTE)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Ticket guardado = ticketRepository.save(nuevoTicket);
        notificarCambiosEnCola();

        return convertirADTO(guardado);
    }

    // ==========================================
    // 2. LÓGICA OPERATIVA (RRHH)
    // ==========================================

    public List<TicketResponseDTO> listarTicketsPendientes() {
        // Usamos el metodo ordenado
        return ticketRepository.findByEstadoOrderByCategoriaAscFechaCreacionAsc(EstadoTicket.PENDIENTE)
                .stream().map(this::convertirADTO).collect(Collectors.toList());
    }

    public List<TicketResponseDTO> listarTicketsEnAtencion() {
        return ticketRepository.findByEstado(EstadoTicket.EN_ATENCION)
                .stream().map(this::convertirADTO).collect(Collectors.toList());
    }

    @Transactional
    public TicketResponseDTO iniciarAtencion(Long ticketId, String usernameAtendedor) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket no encontrado"));

        Usuario usuario = usuarioRepository.findByUsername(usernameAtendedor)
                .orElseThrow(() -> new EntityNotFoundException("Usuario de atención no encontrado"));

        if (ticket.getEstado() != EstadoTicket.PENDIENTE) {
            throw new IllegalStateException("El ticket ya está siendo atendido o finalizó");
        }

        ticket.setEstado(EstadoTicket.EN_ATENCION);
        ticket.setUsuarioAtencion(usuario);
        ticket.setFechaInicioAtencion(LocalDateTime.now());

        Ticket guardado = ticketRepository.save(ticket);
        notificarCambiosEnCola(); // Actualiza la TV (Mueve de espera a pantalla principal)

        return convertirADTO(guardado);
    }

    @Transactional
    public TicketResponseDTO finalizarAtencion(Long ticketId, TicketFinalizacionDTO dto) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket no encontrado"));

        if (ticket.getEstado() != EstadoTicket.EN_ATENCION) {
            throw new IllegalStateException("El ticket debe estar EN_ATENCION para poder finalizarlo");
        }

        // --- 1. LÓGICA DE CORRECCIÓN DE CATEGORÍA ---
        // Si Robinson envía una categoría corregida, actualizamos el ticket
        if (dto.getCategoriaCorregida() != null) {
            // Ejemplo: Cambia de ENTREGAS_BOLETAS a QUEJAS_RECLAMOS
            ticket.setCategoria(dto.getCategoriaCorregida());
        }

        // --- 2. VALIDACIÓN DE SUBCATEGORÍA ---
        // Ahora validamos sobre la categoría FINAL (sea la original o la corregida)
        if (ticket.getCategoria() == CategoriaAtencion.QUEJAS_RECLAMOS) {
            if (dto.getSubCategoria() == null && ticket.getSubCategoria() == null) {
                // Si es queja, OBLIGATORIAMENTE necesita subcategoría
                throw new IllegalArgumentException("Al clasificar como QUEJA/RECLAMO, debe seleccionar una subcategoría.");
            }
        } else {
            // Si NO es queja, limpiamos la subcategoría para no dejar basura
            ticket.setSubCategoria(null);
        }

        // --- 3. CIERRE NORMAL ---
        ticket.setEstado(EstadoTicket.FINALIZADO);
        ticket.setFechaFinAtencion(LocalDateTime.now());

        if (dto.getSubCategoria() != null) {
            ticket.setSubCategoria(dto.getSubCategoria());
        }

        // Formateo de observación
        if (dto.getObservacion() != null && !dto.getObservacion().isEmpty()) {
            String obsActual = (ticket.getObservacion() == null) ? "" : ticket.getObservacion();
            String separador = obsActual.isEmpty() ? "" : " | ";
            ticket.setObservacion(obsActual + separador + "Cierre: " + dto.getObservacion());
        }

        Ticket guardado = ticketRepository.save(ticket);
        notificarCambiosEnCola();

        return convertirADTO(guardado);
    }

    // ==========================================
    // 3. REPORTES Y BÚSQUEDA AVANZADA (PAGINACIÓN)
    // ==========================================

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ResumenAtencionDTO> obtenerHistorialPorDni(String dni) {
        return ticketRepository.contarVisitasPorDni(dni);
    }

    // Historial Paginado Simple
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<TicketResponseDTO> obtenerHistorialPaginado(String dni, Pageable pageable) {
        return ticketRepository.findByDniSolicitante(dni, pageable)
                .map(this::convertirADTO);
    }

    // Historial General Paginado (Para el Jefe)
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<TicketResponseDTO> obtenerTodosLosTickets(Pageable pageable) {
        return ticketRepository.findAll(pageable)
                .map(this::convertirADTO);
    }

    // Buscador Maestro con Filtros Dinámicos
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<TicketResponseDTO> buscarTickets(
            String dni,
            EstadoTicket estado,
            CategoriaAtencion categoria,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Pageable pageable) {

        // Convertir LocalDate a LocalDateTime para cubrir todo el día (00:00 a 23:59)
        LocalDateTime inicio = (fechaDesde != null) ? fechaDesde.atStartOfDay() : null;
        LocalDateTime fin = (fechaHasta != null) ? fechaHasta.atTime(LocalTime.MAX) : null;

        return ticketRepository.buscarConFiltros(dni, estado, categoria, inicio, fin, pageable)
                .map(this::convertirADTO);
    }

    // ==========================================
    // 4. CARGA MASIVA (EXCEL)
    // ==========================================

    @Transactional
    public String importarTicketsMasivos(MultipartFile archivo,
                                         LugarAtencion lugar,
                                         CategoriaAtencion categoria,
                                         String usernameResponsable) throws IOException {

        Usuario responsable = usuarioRepository.findByUsername(usernameResponsable)
                .orElseThrow(() -> new IllegalArgumentException("Usuario responsable no encontrado"));

        List<Ticket> ticketsParaGuardar = new ArrayList<>();
        int filasIgnoradas = 0;
        int filasDuplicadas = 0;
        int filasExitosas = 0;

        // Definir rango "HOY" para detectar duplicados
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = LocalDate.now().atTime(LocalTime.MAX);

        try (Workbook workbook = new XSSFWorkbook(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter dataFormatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Lectura segura de celdas
                Cell celdaDni = row.getCell(0);
                String dni = dataFormatter.formatCellValue(celdaDni).trim();

                Cell celdaObs = row.getCell(1);
                String observacionExcel = (celdaObs != null) ? dataFormatter.formatCellValue(celdaObs) : "";

                // 1. Validaciones básicas
                if (dni.isEmpty() || dni.length() != 8) {
                    filasIgnoradas++;
                    continue;
                }

                // 2. Validar existencia del trabajador
                Optional<Usuario> trabajadorOpt = usuarioRepository.findByUsername(dni);
                if (trabajadorOpt.isEmpty()) {
                    filasIgnoradas++;
                    System.out.println("⚠️ DNI no encontrado: " + dni);
                    continue;
                }

                // 3. Validar DUPLICADOS (Regla: 1 ticket por categoría por día en carga masiva)
                boolean yaExiste = ticketRepository.existsByDniSolicitanteAndCategoriaAndFechaCreacionBetween(
                        dni, categoria, inicioDia, finDia
                );

                if (yaExiste) {
                    filasDuplicadas++;
                    System.out.println("🔁 Duplicado omitido: " + dni);
                    continue;
                }

                // 4. Crear Ticket (Directo a FINALIZADO)
                LocalDateTime ahora = LocalDateTime.now();

                Ticket ticket = Ticket.builder()
                        .dniSolicitante(dni)
                        .lugarAtencion(lugar)
                        .categoria(categoria)
                        .observacion("Carga Masiva Excel. " + observacionExcel)
                        .estado(EstadoTicket.FINALIZADO) // No pasa por cola
                        .fechaCreacion(ahora)
                        .fechaInicioAtencion(ahora)
                        .fechaFinAtencion(ahora)
                        .usuarioAtencion(responsable)
                        .subCategoria(null)
                        .build();

                ticketsParaGuardar.add(ticket);
                filasExitosas++;
            }
        }

        ticketRepository.saveAll(ticketsParaGuardar);

        // No notificamos por WebSocket porque son históricos/regularizaciones

        return String.format("Proceso completado. Creados: %d. Duplicados omitidos: %d. Ignorados (Error DNI): %d.",
                filasExitosas, filasDuplicadas, filasIgnoradas);
    }

    @Transactional
    public TicketResponseDTO editarTicket(Long id, TicketEdicionDTO dto) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket no encontrado"));

        // Regla de Negocio: Solo se puede editar si NO ha finalizado
        if (ticket.getEstado() == EstadoTicket.FINALIZADO) {
            throw new IllegalStateException("No se puede editar un ticket que ya fue finalizado. Cree uno nuevo.");
        }

        // Actualizamos solo lo que venga en el JSON
        if (dto.getCategoria() != null) {
            ticket.setCategoria(dto.getCategoria());
            // Si cambia la categoría, reseteamos subcategoría por seguridad
            ticket.setSubCategoria(null);
        }

        if (dto.getObservacion() != null && !dto.getObservacion().isEmpty()) {
            ticket.setObservacion(dto.getObservacion());
        }

        Ticket guardado = ticketRepository.save(ticket);

        // ¡IMPORTANTE! Notificar a la TV porque la información cambió
        notificarCambiosEnCola();

        return convertirADTO(guardado);
    }

    // ==========================================
    // 5. RECUPERACIÓN DE SESIÓN (F5 FIX)
    // ==========================================

    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<TicketResponseDTO> buscarTicketActivoPorUsuario(String username) {
        return ticketRepository.findByUsuarioAtencion_UsernameAndEstado(username, EstadoTicket.EN_ATENCION)
                .map(this::convertirADTO);
    }

    @Transactional
    public TicketResponseDTO cancelarTicket(Long ticketId, String observacion) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket no encontrado"));

        // Validar que el ticket no esté ya finalizado o cancelado
        if (ticket.getEstado() == EstadoTicket.FINALIZADO) {
            throw new IllegalStateException("No se puede cancelar un ticket que ya fue finalizado.");
        }

        if (ticket.getEstado() == EstadoTicket.CANCELADO) {
            throw new IllegalStateException("El ticket ya está cancelado.");
        }

        // Permitir cancelar tickets PENDIENTES o EN_ATENCION
        ticket.setEstado(EstadoTicket.CANCELADO);
        ticket.setFechaFinAtencion(LocalDateTime.now());

        // Guardamos el motivo de la cancelación
        if (observacion != null && !observacion.isEmpty()) {
            String obsActual = (ticket.getObservacion() == null) ? "" : ticket.getObservacion();
            ticket.setObservacion(obsActual + " | Cancelado: " + observacion);
        }

        Ticket guardado = ticketRepository.save(ticket);
        notificarCambiosEnCola(); // Actualiza la pantalla para que desaparezca

        return convertirADTO(guardado);
    }
}
