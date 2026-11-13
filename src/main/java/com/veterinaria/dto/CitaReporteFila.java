package com.veterinaria.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila mínima que se lee de la base para armar el reporte. */
public record CitaReporteFila(
        String servicio,
        String estado,
        BigDecimal precio,
        LocalDateTime fechaHoraInicio) {
}