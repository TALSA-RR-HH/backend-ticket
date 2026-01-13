package com.talsa.rrhh.backend.dto;

import com.talsa.rrhh.backend.enums.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TicketResponseDTO {

    private Long id;
    private String dniSolicitante;
    private LugarAtencion lugarAtencion;
    private CategoriaAtencion categoria;
    private SubCategoriaQueja subCategoria;
    private String observacion;
    private EstadoTicket estado;
    private String nombreSolicitante;

    // CAMBIO CLAVE: Ya no enviamos el objeto Usuario, solo el nombre formateado
    private String trabajadorAtencion;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaInicioAtencion;
    private LocalDateTime fechaFinAtencion;
}
