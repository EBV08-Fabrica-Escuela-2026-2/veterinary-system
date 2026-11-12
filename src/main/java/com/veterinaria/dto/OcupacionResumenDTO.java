package com.veterinaria.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcupacionResumenDTO {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String agrupacion;
    private Long totalSlotsHabilitados;
    private Long totalCitasEfectivas;
    private Long totalSlotsDisponibles;
    private BigDecimal porcentajeOcupacion;
    private OcupacionProfesionalDTO mayorDemanda;
    private OcupacionProfesionalDTO menorDemanda;
    private List<OcupacionDistribucionDTO> distribucion;
    private String mensaje;
}
