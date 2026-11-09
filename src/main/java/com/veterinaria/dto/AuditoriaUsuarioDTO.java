package com.veterinaria.dto;

import java.time.LocalDateTime;

/** Cambio auditado sobre un usuario: qué cambió, quién lo hizo y cuándo. */
public record AuditoriaUsuarioDTO(
        Long id,
        String accion,
        String valorAnterior,
        String valorNuevo,
        Long administradorId,
        String administradorCorreo,
        LocalDateTime fecha) {
}
