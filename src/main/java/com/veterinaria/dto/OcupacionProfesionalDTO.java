package com.veterinaria.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcupacionProfesionalDTO {

    private Long veterinarioId;
    private String nombre;
    private Long slotsHabilitados;
    private Long citasEfectivas;
    private Long slotsDisponibles;
    private BigDecimal porcentajeOcupacion;
}
