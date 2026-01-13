package com.talsa.rrhh.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketCancelacionDTO {

    @NotBlank(message = "Debe especificar un motivo para la cancelación")
    @Size(max = 500, message = "El motivo no puede exceder 500 caracteres")
    private String observacion;
}