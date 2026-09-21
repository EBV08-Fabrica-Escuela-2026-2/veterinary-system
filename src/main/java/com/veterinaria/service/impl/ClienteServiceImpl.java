package com.veterinaria.service.impl;

import com.veterinaria.dto.ClienteRegistroDTO;
import com.veterinaria.dto.ClienteResponseDTO;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.Cliente;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.service.ClienteService;
import org.springframework.stereotype.Service;

@Service
public class ClienteServiceImpl implements ClienteService {
    private final ClienteRepository clienteRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public Cliente registrarCliente(ClienteRegistroDTO dto) {
        if (clienteRepository.existsByDocumentoIdentidad(dto.getDocumentoIdentidad())) {
            throw new IllegalArgumentException("Este cliente ya se encuentra registrado");
        }

        if (clienteRepository.existsByCorreo(dto.getCorreo())) {
            throw new IllegalArgumentException("Este correo ya se encuentra registrado");
        }

        Cliente cliente = Cliente.builder()
                .nombre(dto.getNombre())
                .documentoIdentidad(dto.getDocumentoIdentidad())
                .telefono(dto.getTelefono())
                .correo(dto.getCorreo())
                .direccion(dto.getDireccion())
                .activo(true)
                .build();

        return clienteRepository.save(cliente);
    }

    @Override
    public ClienteResponseDTO consultarClientePorDocumento(String documentoIdentidad) {
        Cliente cliente = clienteRepository.findByDocumentoIdentidad(documentoIdentidad)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente no encontrado con documento: " + documentoIdentidad));

        return ClienteResponseDTO.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .documentoIdentidad(cliente.getDocumentoIdentidad())
                .telefono(cliente.getTelefono())
                .correo(cliente.getCorreo())
                .direccion(cliente.getDireccion())
                .activo(cliente.getActivo())
                .build();
    }
}
