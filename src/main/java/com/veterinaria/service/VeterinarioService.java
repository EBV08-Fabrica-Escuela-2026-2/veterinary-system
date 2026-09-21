package com.veterinaria.service;

import java.util.List;

import com.veterinaria.dto.VeterinarioListaDTO;
import com.veterinaria.dto.VeterinarioRegistroDTO;
import com.veterinaria.model.Veterinario;

public interface VeterinarioService {
    Veterinario registrarVeterinario(VeterinarioRegistroDTO dto);

    List<VeterinarioListaDTO> listarActivos();

}
