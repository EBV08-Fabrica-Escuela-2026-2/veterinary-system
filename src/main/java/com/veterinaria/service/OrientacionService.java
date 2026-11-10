package com.veterinaria.service;

import com.veterinaria.dto.OrientacionRequestDTO;
import com.veterinaria.dto.OrientacionResponseDTO;

public interface OrientacionService {

    OrientacionResponseDTO orientar(String correoCliente, OrientacionRequestDTO solicitud);
}
