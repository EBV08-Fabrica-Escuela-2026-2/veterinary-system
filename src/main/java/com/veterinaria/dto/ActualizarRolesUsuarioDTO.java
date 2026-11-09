package com.veterinaria.dto;

import java.util.Set;

import com.veterinaria.model.Rol;

import jakarta.validation.constraints.NotEmpty;

public record ActualizarRolesUsuarioDTO(
        @NotEmpty(message = "Selecciona al menos un rol") Set<Rol> roles) {
}
