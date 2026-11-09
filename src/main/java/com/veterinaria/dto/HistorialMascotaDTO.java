package com.veterinaria.dto;

import java.util.List;

public record HistorialMascotaDTO(
        Long mascotaId,
        String mascotaNombre,
        List<HistorialCitaDTO> contenido,
        int pagina,
        int limite,
        long totalElementos,
        int totalPaginas) {
}
