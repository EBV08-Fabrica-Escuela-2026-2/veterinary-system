package com.veterinaria.dto;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoUsuarioDTO(
        @NotNull(message = "El estado es obligatorio") Boolean activo) {
}
