package com.veterinaria.dto;

import java.time.LocalTime;

public record FranjaDisponibilidadDTO(Long id, String dia, LocalTime horaInicio, LocalTime horaFin) {
}
