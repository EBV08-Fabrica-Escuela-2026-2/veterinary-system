package com.veterinaria.service.impl;

import com.veterinaria.dto.ClienteRegistroDTO;
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

        Cliente clienteExistentePorDocumento = documento != null
                ? clienteRepository.findByDocumentoIdentidad(documento).orElse(null)
                : null;

        if (clienteExistentePorDocumento != null) {
            return clienteExistentePorDocumento;
        }

        Cliente clienteExistentePorCorreo = correo != null
                ? clienteRepository.findByCorreo(correo).orElse(null)
                : null;

        if (clienteExistentePorCorreo != null) {
            return clienteExistentePorCorreo;
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
}
