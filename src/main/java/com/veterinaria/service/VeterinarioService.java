package com.veterinaria.service;

import com.veterinaria.dto.VeterinarioRegistroDTO;
import com.veterinaria.model.Veterinario;

public interface VeterinarioService {
    Veterinario registrarVeterinario(VeterinarioRegistroDTO dto);

}
