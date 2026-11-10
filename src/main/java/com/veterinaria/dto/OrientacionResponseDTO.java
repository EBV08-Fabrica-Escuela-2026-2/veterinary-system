package com.veterinaria.dto;

import java.util.List;

public record OrientacionResponseDTO(
        boolean urgente,
        String mensajeUrgencia,
        boolean modoRespaldo,
        String mensaje,
        List<ServicioRecomendadoDTO> recomendaciones,
        String advertencia) {

    public record ServicioRecomendadoDTO(
            Long servicioId,
            String nombre,
            String explicacion,
            List<VeterinarioSugeridoDTO> veterinarios) {
    }

    public record VeterinarioSugeridoDTO(Long id, String nombre) {
    }
}
