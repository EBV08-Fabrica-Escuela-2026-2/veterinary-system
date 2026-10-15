package com.veterinaria.service;

import com.veterinaria.dto.DisponibilidadDiaDTO;

import java.time.LocalDate;
import java.util.List;

public interface DisponibilidadService {
    DisponibilidadDiaDTO consultarDisponibilidad(Long servicioId, Long veterinarioId, LocalDate fecha);

    List<DisponibilidadDiaDTO> consultarDisponibilidadRango(Long servicioId, Long veterinarioId, LocalDate fechaInicio,
            LocalDate fechaFin);
}
