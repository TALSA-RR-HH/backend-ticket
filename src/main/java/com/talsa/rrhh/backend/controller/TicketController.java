package com.talsa.rrhh.backend.controller;

import com.talsa.rrhh.backend.dto.*;
import com.talsa.rrhh.backend.enums.*;
import com.talsa.rrhh.backend.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Permite conexiones desde cualquier Frontend (React/Angular/Movil)
public class TicketController {

    private final TicketService ticketService;

    // ==========================================
    // 1. REGISTRO (TABLET)
    // ==========================================

    @PostMapping("/registro")
    public ResponseEntity<TicketResponseDTO> registrarTicket(@Valid @RequestBody TicketRegistroDTO dto) {
        TicketResponseDTO ticketCreado = ticketService.registrarSolicitud(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketCreado);
    }

    // ==========================================
    // 2. LISTAS OPERATIVAS (PANTALLAS / WEB)
    // ==========================================

    @GetMapping("/pendientes")
    public ResponseEntity<List<TicketResponseDTO>> listarPendientes() {
        return ResponseEntity.ok(ticketService.listarTicketsPendientes());
    }

    @GetMapping("/en-atencion")
    public ResponseEntity<List<TicketResponseDTO>> listarEnAtencion() {
        return ResponseEntity.ok(ticketService.listarTicketsEnAtencion());
    }

    // ==========================================
    // 3. GESTIÓN DE ATENCIÓN (RRHH)
    // ==========================================

    @PostMapping("/{id}/iniciar")
    public ResponseEntity<TicketResponseDTO> iniciarAtencion(@PathVariable Long id, Principal principal) {
        TicketResponseDTO ticket = ticketService.iniciarAtencion(id, principal.getName());
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<TicketResponseDTO> finalizarAtencion(@PathVariable Long id, @RequestBody TicketFinalizacionDTO dto) {
        // Pasamos el DTO completo al servicio
        TicketResponseDTO ticket = ticketService.finalizarAtencion(id, dto);

        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/mi-asignacion")
    public ResponseEntity<TicketResponseDTO> obtenerTicketEnAtencionActual() {
        // 1. Obtener quién está logueado (Spring Security)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // 2. Preguntar al servicio
        return ticketService.buscarTicketActivoPorUsuario(username)
                .map(ResponseEntity::ok) // Si hay ticket, devuelve 200 OK + JSON
                .orElse(ResponseEntity.noContent().build()); // Si no hay, devuelve 204 No Content
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<TicketResponseDTO> cancelarTicket(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String motivo = (body != null && body.containsKey("observacion")) ? body.get("observacion") : "Sin motivo especificado";
        TicketResponseDTO ticket = ticketService.cancelarTicket(id, motivo);
        return ResponseEntity.ok(ticket);
    }

    // ==========================================
    // 4. HISTORIAL Y REPORTES (PAGINADOS)
    // ==========================================

    // Historial personal por DNI
    @GetMapping("/historial/{dni}")
    public ResponseEntity<PaginaResponseDTO<TicketResponseDTO>> obtenerHistorialUsuario(
            @PathVariable String dni,
            @RequestParam(defaultValue = "false") boolean todo,
            @PageableDefault(page = 0, size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Pageable paginacionFinal = todo ? Pageable.unpaged() : pageable;
        Page<TicketResponseDTO> paginaSpring = ticketService.obtenerHistorialPaginado(dni, paginacionFinal);

        // Envolvemos la respuesta en nuestro DTO limpio
        return ResponseEntity.ok(new PaginaResponseDTO<>(paginaSpring));
    }

    // Historial General (Para el Jefe)
    @GetMapping("/historial-general")
    public ResponseEntity<PaginaResponseDTO<TicketResponseDTO>> obtenerHistorialGeneral(
            @RequestParam(defaultValue = "false") boolean todo,
            @PageableDefault(page = 0, size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Pageable paginacionFinal = todo ? Pageable.unpaged() : pageable;
        Page<TicketResponseDTO> paginaSpring = ticketService.obtenerTodosLosTickets(paginacionFinal);

        return ResponseEntity.ok(new PaginaResponseDTO<>(paginaSpring));
    }

    // Buscador Maestro (Filtros dinámicos)
    @GetMapping("/buscar")
    public ResponseEntity<PaginaResponseDTO<TicketResponseDTO>> buscarTickets(@RequestParam(required = false) String dni,
                                                                              @RequestParam(required = false) EstadoTicket estado,
                                                                              @RequestParam(required = false) CategoriaAtencion categoria,
                                                                              @RequestParam(required = false) LocalDate fechaDesde,
                                                                              @RequestParam(required = false) LocalDate fechaHasta,
                                                                              @RequestParam(defaultValue = "false") boolean todo,
                                                                              @PageableDefault(page = 0, size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
        // Lógica: Si 'todo' es true, usamos un paginador "infinito". Si no, usamos el normal.
        Pageable paginacionFinal = todo ? Pageable.unpaged() : pageable;

        // Llamamos al servicio con los filtros
        Page<TicketResponseDTO> paginaSpring = ticketService.buscarTickets(
                dni, estado, categoria, fechaDesde, fechaHasta, paginacionFinal
        );

        // Envolvemos la respuesta en nuestro DTO limpio (sin basura técnica)
        return ResponseEntity.ok(new PaginaResponseDTO<>(paginaSpring));
    }

    // Reporte de conteo simple (KPIs)
    @GetMapping("/reporte/resumen/{dni}")
    public ResponseEntity<List<ResumenAtencionDTO>> obtenerResumenAtencion(@PathVariable String dni) {
        return ResponseEntity.ok(ticketService.obtenerHistorialPorDni(dni));
    }

    // ==========================================
    // 5. CARGA MASIVA (EXCEL)
    // ==========================================

    @PostMapping("/importar")
    @PreAuthorize("hasAnyAuthority('PERSONAL_RRHH', 'JEFE_RRHH', 'ADMIN_TI')")
    public ResponseEntity<String> importarTickets(@RequestParam("file") MultipartFile file,
                                                  @RequestParam("lugarAtencion") LugarAtencion lugarAtencion,
                                                  @RequestParam("categoria") CategoriaAtencion categoria,
                                                  Principal principal) {
        // Validar tamaño del archivo (máximo 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body("El archivo no puede superar los 5MB");
        }

        try {
            String resultado = ticketService.importarTicketsMasivos(file, lugarAtencion, categoria, principal.getName());
            return ResponseEntity.ok(resultado);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar el archivo Excel: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PERSONAL_RRHH', 'JEFE_RRHH', 'ADMIN_TI')")
    public ResponseEntity<TicketResponseDTO> editarTicket(@PathVariable Long id, @Valid @RequestBody TicketEdicionDTO dto) {
        TicketResponseDTO ticketActualizado = ticketService.editarTicket(id, dto);
        return ResponseEntity.ok(ticketActualizado);
    }
}
