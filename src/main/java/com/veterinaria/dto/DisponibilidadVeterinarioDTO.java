package com.veterinaria.dto;

import java.time.LocalTime;
import java.util.List;

public record DisponibilidadVeterinarioDTO(Long veterinarioId, List<Dia> dias) {
    public record Dia(String dia, Boolean activo, List<Franja> franjas) {
    }

    public record Franja(Long id, LocalTime horaInicio, LocalTime horaFin) {
    }
}
