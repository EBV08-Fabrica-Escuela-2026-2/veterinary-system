package com.veterinaria.service;

import com.veterinaria.dto.CatalogoResponseDTO;
import com.veterinaria.dto.VeterinarioDetalleDTO;

public interface CatalogoService {
    CatalogoResponseDTO consultarCatalogo();
    VeterinarioDetalleDTO consultarDetalleVeterinario(Long veterinarioId);
}
