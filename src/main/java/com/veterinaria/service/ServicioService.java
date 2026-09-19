package com.veterinaria.service;

import com.veterinaria.dto.ServicioRegistroDTO;
import com.veterinaria.model.Servicio;

public interface ServicioService {
    Servicio registrarServicio(ServicioRegistroDTO dto);
}
