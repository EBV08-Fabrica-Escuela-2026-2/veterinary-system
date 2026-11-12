package com.veterinaria.service;

import java.time.LocalDate;

import com.veterinaria.dto.OcupacionResumenDTO;

public interface OcupacionService {

    OcupacionResumenDTO consultarResumen(LocalDate fechaInicio, LocalDate fechaFin, Long veterinarioId, Long sedeId,
            String agrupacion);

    OcupacionResumenDTO consultarDistribucion(LocalDate fechaInicio, LocalDate fechaFin, Long veterinarioId, Long sedeId,
            String agrupacion);
}
