package com.veterinaria.service;

import com.veterinaria.dto.CancelacionCitaDTO;
import com.veterinaria.dto.CitaClienteDTO;
import com.veterinaria.dto.CitaDetalleDTO;
import com.veterinaria.dto.CitaRegistroDTO;
import com.veterinaria.dto.DisponibilidadDiaDTO;
import com.veterinaria.dto.ReprogramacionCitaResponseDTO;
import com.veterinaria.dto.ReprogramarCitaRequestDTO;
import com.veterinaria.model.Cita;

import java.util.List;
import java.time.LocalDate;

public interface CitaService {
    Cita reservarCita(CitaRegistroDTO dto, Long clienteId);

    List<CitaClienteDTO> listarCitasPorCliente(Long clienteId);

    CancelacionCitaDTO cancelarCita(Long citaId, Long clienteId);

    CitaDetalleDTO obtenerDetalle(Long citaId, Long clienteId);

    DisponibilidadDiaDTO consultarDisponibilidadReprogramacion(Long citaId, Long clienteId, LocalDate fecha);

    ReprogramacionCitaResponseDTO reprogramarCita(Long citaId, Long clienteId, ReprogramarCitaRequestDTO solicitud);
}
