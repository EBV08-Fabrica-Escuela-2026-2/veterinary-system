package com.veterinaria.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Fila de la tabla de administración de usuarios. */
public record UsuarioAdminDTO(
        Long id,
        String nombre,
        String correo,
        String rol,
        List<String> roles,
        boolean activo,
        Long clienteId,
        Long veterinarioId,
        LocalDateTime actualizadoEn) {
}
