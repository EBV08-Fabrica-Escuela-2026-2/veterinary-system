package com.veterinaria.service;

import com.veterinaria.dto.MascotaRegistroDTO;
import com.veterinaria.model.Mascota;

import java.util.List;

public interface MascotaService {

    Mascota registrarMascota(MascotaRegistroDTO dto, Long clienteId);

    List<Mascota> listarMascotasPorCliente(Long clienteId);
}
