package com.talsa.rrhh.backend.repository;

import com.talsa.rrhh.backend.dto.ResumenAtencionDTO;
import com.talsa.rrhh.backend.entity.Ticket;
import com.talsa.rrhh.backend.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // --- CONSULTAS OPERATIVAS (Día a día) ---

    // Verifica si existe algún ticket para este DNI con el estado específico (PENDIENTE)
    boolean existsByDniSolicitanteAndEstado(String dniSolicitante, EstadoTicket estado);

    // 1. Ver la cola de espera: Tickets que están PENDIENTES
    List<Ticket> findByEstado(EstadoTicket estado);

    // 2. Ver historial completo de un trabajador por su DNI
    List<Ticket> findByDniSolicitante(String dniSolicitante);

    // 3. Filtrar por Lugar (Ej. Ver solo tickets generados en "CAMPO" o "WHATSAPP")
    List<Ticket> findByLugarAtencion(LugarAtencion lugarAtencion);

    // --- CONSULTAS PARA REPORTES Y KPIs (Jefe/Admin) ---

    /**
     * REPORTE EJECUTIVO: Cuenta cuántas veces ha venido un trabajador agrupado por motivo.
     * Útil para responder: "¿Cuántas veces vino este DNI por 'Boletas'?"
     * Devuelve un DTO personalizado, no la entidad completa.
     */
    @Query("SELECT new com.talsa.rrhh.backend.dto.ResumenAtencionDTO(t.categoria, COUNT(t)) " +
            "FROM Ticket t " +
            "WHERE t.dniSolicitante = :dni " +
            "GROUP BY t.categoria")
    List<ResumenAtencionDTO> contarVisitasPorDni(@Param("dni") String dni);

    /**
     * REPORTE MENSUAL: Busca tickets dentro de un rango de fechas.
     * Indispensable para el botón "Exportar a Excel" del mes.
     */
    List<Ticket> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);

    /**
     * RANKING DE PRODUCTIVIDAD: Busca tickets atendidos por un usuario específico de RRHH.
     * Sirve para saber cuántos tickets atendió Robinson vs Yesenia (KPI de desempeño).
     */
    List<Ticket> findByUsuarioAtencion_Id(Long usuarioId);

    // Verifica si ya existe un ticket para este DNI, en esta Categoría, dentro de un rango de fechas (Hoy)
    boolean existsByDniSolicitanteAndCategoriaAndFechaCreacionBetween(
            String dni,
            CategoriaAtencion categoria,
            LocalDateTime inicio,
            LocalDateTime fin
    );

    // En lugar de devolver TODA la lista, devolvemos una "Página"
    Page<Ticket> findByDniSolicitante(String dniSolicitante, Pageable pageable);

    // Si quisieras ver todos los tickets del sistema paginados:
    Page<Ticket> findAll(Pageable pageable);

    // --- NUEVO: FILTRO DINÁMICO MAESTRO ---
    @Query("SELECT t FROM Ticket t WHERE " +
            "(:dni IS NULL OR t.dniSolicitante LIKE %:dni%) AND " +
            "(:estado IS NULL OR t.estado = :estado) AND " +
            "(:categoria IS NULL OR t.categoria = :categoria) AND " +
            "(:fechaInicio IS NULL OR t.fechaCreacion >= :fechaInicio) AND " +
            "(:fechaFin IS NULL OR t.fechaCreacion <= :fechaFin)")
    Page<Ticket> buscarConFiltros(
            @Param("dni") String dni,
            @Param("estado") EstadoTicket estado,
            @Param("categoria") CategoriaAtencion categoria,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );

    boolean existsByDniSolicitanteAndEstadoIn(String dni, List<EstadoTicket> estados);

    // Recuperación de Sesión: Busca si este usuario tiene algo pendiente en su escritorio
    Optional<Ticket> findByUsuarioAtencion_UsernameAndEstado(String username, EstadoTicket estado);

    // Reemplaza o agrega este método:
    // Ordena por Categoría (Agrupación) y luego por antigüedad (FIFO)
    List<Ticket> findByEstadoOrderByCategoriaAscFechaCreacionAsc(EstadoTicket estado);

    // Traer tickets que estén en una lista de estados (PENDIENTE o AUSENTE)
    // Ordenados: Agrupados por Categoría y luego por orden de llegada
    List<Ticket> findByEstadoInOrderByCategoriaAscFechaCreacionAsc(List<EstadoTicket> estados);
}
