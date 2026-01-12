package com.talsa.rrhh.backend.dto;

import com.talsa.rrhh.backend.enums.CategoriaAtencion;
import com.talsa.rrhh.backend.enums.LugarAtencion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketRegistroDTO {

    // Validación: DNI debe tener 8 dígitos numéricos
    @NotNull(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
    private String dniSolicitante;

    @NotNull(message = "El lugar de atención es obligatorio")
    private LugarAtencion lugarAtencion;

    @NotNull(message = "La categoría es obligatoria")
    private CategoriaAtencion categoria;

    // Opcional: El usuario podría escribir algo breve, o dejarlo vacío
    @Size(max = 500, message = "La observación no puede exceder 500 caracteres")
    private String observacion;
}
