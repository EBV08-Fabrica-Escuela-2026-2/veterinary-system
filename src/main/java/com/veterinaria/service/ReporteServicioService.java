package com.veterinaria.service;

import java.time.YearMonth;

import com.veterinaria.dto.UsoServiciosReporteDTO;

public interface ReporteServicioService {

    /**
     * Consolidado de uso de servicios de un mes.
     *
     * @param mes      mes a consultar; si es null, el mes en curso
     * @param servicio nombre del servicio clínico; si es null o vacío, todos
     */
    UsoServiciosReporteDTO usoDeServicios(YearMonth mes, String servicio);
}