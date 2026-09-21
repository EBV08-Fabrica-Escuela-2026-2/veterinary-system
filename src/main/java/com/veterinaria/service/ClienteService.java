package com.veterinaria.service;

import com.veterinaria.dto.ClienteRegistroDTO;
import com.veterinaria.dto.ClienteResponseDTO;
import com.veterinaria.model.Cliente;

public interface ClienteService {
    Cliente registrarCliente(ClienteRegistroDTO dto);

    ClienteResponseDTO consultarClientePorDocumento(String documentoIdentidad);
}
