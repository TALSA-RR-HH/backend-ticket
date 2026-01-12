package com.talsa.rrhh.backend.dto;

import com.talsa.rrhh.backend.enums.CategoriaAtencion;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketEdicionDTO {

    private CategoriaAtencion categoria;

    @Size(max = 500, message = "La observación no puede exceder 500 caracteres")
    private String observacion;
}
