package com.veterinaria.service;

import com.veterinaria.dto.CancelacionCitaDTO;
import com.veterinaria.dto.CitaClienteDTO;
import com.veterinaria.dto.CitaRegistroDTO;
import com.veterinaria.model.Cita;

import java.util.List;

public interface CitaService {
    Cita reservarCita(CitaRegistroDTO dto, Long clienteId);

    List<CitaClienteDTO> listarCitasPorCliente(Long clienteId);

    CancelacionCitaDTO cancelarCita(Long citaId, Long clienteId);
}