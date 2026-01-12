package com.talsa.rrhh.backend.entity;

import com.talsa.rrhh.backend.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Datos del Solicitante (Trabajador) ---
    @Column(nullable = false, length = 8)
    private String dniSolicitante; // Ingresado en la tablet

    // --- Clasificación del Ticket ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LugarAtencion lugarAtencion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaAtencion categoria;

    @Enumerated(EnumType.STRING)
    private SubCategoriaQueja subCategoria; // Null si categoria != QUEJAS_RECLAMOS

    @Column(columnDefinition = "TEXT")
    private String observacion; // Detalles adicionales

    // --- Gestión del Flujo (Estados y Responsable) ---
    @Enumerated(EnumType.STRING)
    private EstadoTicket estado;

    // Relación: ¿Quién atendió este ticket? (Puede ser null al inicio)
    @ManyToOne
    @JoinColumn(name = "usuario_atencion_id")
    private Usuario usuarioAtencion;

    // --- Métricas de Tiempo (KPIs) ---
    private LocalDateTime fechaCreacion;       // Cuando el trabajador se registra
    private LocalDateTime fechaInicioAtencion; // Cuando RRHH llama al trabajador
    private LocalDateTime fechaFinAtencion;    // Cuando se cierra el caso
}
