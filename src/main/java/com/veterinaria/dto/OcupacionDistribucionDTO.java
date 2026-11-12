package com.veterinaria.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcupacionDistribucionDTO {

    private String periodo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Long slotsHabilitados;
    private Long citasEfectivas;
    private Long slotsDisponibles;
    private BigDecimal porcentajeOcupacion;
}
