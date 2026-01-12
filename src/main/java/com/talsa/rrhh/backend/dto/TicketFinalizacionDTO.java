package com.talsa.rrhh.backend.dto;

import com.talsa.rrhh.backend.enums.CategoriaAtencion;
import com.talsa.rrhh.backend.enums.SubCategoriaQueja;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketFinalizacionDTO {

    @Size(max = 500, message = "La observación no puede exceder 500 caracteres")
    private String observacion;

    private SubCategoriaQueja subCategoria; // Puede ser null si no es Queja
    private CategoriaAtencion categoriaCorregida;
}
