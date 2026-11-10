package com.veterinaria.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OrientacionRequestDTO(
        @NotNull @Positive Long mascotaId,
        @NotBlank @Size(min = 10, max = 1000) String sintomas,
        @NotNull @Min(1) @Max(365) Integer duracionValor,
        @NotNull UnidadDuracion duracionUnidad,
        @NotNull NivelPreocupacion nivelPreocupacion) {

    public enum UnidadDuracion {
        horas,
        dias,
        semanas
    }

    public enum NivelPreocupacion {
        bajo,
        moderado,
        alto
    }
}
