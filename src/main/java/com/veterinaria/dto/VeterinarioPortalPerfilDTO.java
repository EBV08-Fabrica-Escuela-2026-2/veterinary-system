package com.veterinaria.dto;

public record VeterinarioPortalPerfilDTO(
        Long id,
        String nombre,
        String correo,
        String documentoIdentidad,
        String especialidad) {
}
