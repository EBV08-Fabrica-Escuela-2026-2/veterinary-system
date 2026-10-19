package com.veterinaria.dto;

import java.time.LocalDateTime;

public record CitaVeterinarioDTO(
        Long id,
        String clienteNombre,
        String mascotaNombre,
        String mascotaEspecie,
        String mascotaRaza,
        String servicioNombre,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String estado,
        String motivoCancelacion) {
}
