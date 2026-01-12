package com.talsa.rrhh.backend.dto;

import com.talsa.rrhh.backend.enums.CategoriaAtencion;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResumenAtencionDTO {
    private CategoriaAtencion categoria;
    private Long cantidadVisitas;
}
