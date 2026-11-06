package com.veterinaria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActualizarEstadoCitaRequestDTO(
        @NotBlank(message = "El estado es obligatorio")
        @Pattern(regexp = "COMPLETADA|NO_ASISTIO", message = "El estado debe ser COMPLETADA o NO_ASISTIO")
        String estado) {
}
