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
        String documento = dto.getDocumentoIdentidad() != null ? dto.getDocumentoIdentidad().trim() : null;
        String correo = dto.getCorreo() != null ? dto.getCorreo().trim() : null;

        if (documento != null && clienteRepository.findByDocumentoIdentidad(documento).isPresent()) {
            throw new IllegalArgumentException("Este cliente ya se encuentra registrado");
        }

        if (correo != null && clienteRepository.findByCorreo(correo).isPresent()) {
            throw new IllegalArgumentException("Este correo ya se encuentra registrado");
        }

        Cliente cliente = Cliente.builder()
                .nombre(dto.getNombre())
                .documentoIdentidad(documento)
                .telefono(dto.getTelefono())
                .correo(correo)
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
