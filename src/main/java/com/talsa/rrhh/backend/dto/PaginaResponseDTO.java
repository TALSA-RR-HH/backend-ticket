package com.talsa.rrhh.backend.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PaginaResponseDTO<T> {

    private List<T> contenido;       // La lista de tickets
    private int paginaActual;        // 0, 1, 2...
    private int itemsPorPagina;      // 10, 20...
    private long totalElementos;     // Total real en BD (ej. 500)
    private int totalPaginas;        // Total de páginas calculadas

    // Constructor inteligente que extrae solo lo útil de Spring Page
    public PaginaResponseDTO(Page<T> page) {
        this.contenido = page.getContent();
        this.paginaActual = page.getNumber();
        this.itemsPorPagina = page.getSize();
        this.totalElementos = page.getTotalElements();
        this.totalPaginas = page.getTotalPages();
    }
}
