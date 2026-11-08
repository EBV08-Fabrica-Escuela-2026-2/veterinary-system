package com.veterinaria.dto;

import java.time.LocalDateTime;

public record EstadoCitaResponseDTO(
        Long idCita,
        String estado,
        String etiquetaEstado,
        String estadoTexto,
        String mensaje,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin) {
}
