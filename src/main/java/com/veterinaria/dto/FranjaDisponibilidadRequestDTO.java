package com.veterinaria.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FranjaDisponibilidadRequestDTO(
        @NotBlank(message = "El día de la semana es obligatorio") String dia,
        @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
        @NotNull(message = "La hora de fin es obligatoria") LocalTime horaFin) {
}
