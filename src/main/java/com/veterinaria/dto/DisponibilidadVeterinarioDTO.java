package com.veterinaria.dto;

import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DisponibilidadVeterinarioDTO(
        Long veterinarioId,
        @NotNull(message = "La disponibilidad es obligatoria") List<@Valid Dia> dias) {

    public record Dia(
            @NotBlank(message = "El día de la semana es obligatorio") String dia,
            Boolean activo,
            List<@Valid Franja> franjas) {
    }

    public record Franja(
            Long id,
            @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
            @NotNull(message = "La hora de fin es obligatoria") LocalTime horaFin) {
    }
}
