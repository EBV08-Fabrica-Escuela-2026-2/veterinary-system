package com.veterinaria.service;

import com.veterinaria.dto.CitaRegistroDTO;
import com.veterinaria.model.Cita;

public interface CitaService {
    Cita reservarCita(CitaRegistroDTO dto, Long clienteId);
}