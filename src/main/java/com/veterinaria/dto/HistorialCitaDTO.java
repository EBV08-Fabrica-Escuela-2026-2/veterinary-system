package com.veterinaria.dto;

import java.time.LocalDateTime;

public record HistorialCitaDTO(
        Long idCita,
        String estado,
        String etiquetaEstado,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String veterinarioNombre,
        String veterinarioEspecialidad,
        String servicioNombre,
        String servicioCategoria,
        String diagnostico,
        String tratamiento,
        String notasClinicas,
        String motivoCancelacion,
        LocalDateTime canceladaAt,
        String canceladaPorVeterinario) {
}
