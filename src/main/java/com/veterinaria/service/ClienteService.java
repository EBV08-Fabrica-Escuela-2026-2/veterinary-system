package com.veterinaria.service;

import com.veterinaria.dto.ClienteRegistroDTO;
import com.veterinaria.model.Cliente;

public interface ClienteService {
    Cliente registrarCliente(ClienteRegistroDTO dto);
}
