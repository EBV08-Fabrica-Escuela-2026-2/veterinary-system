package com.veterinaria.service.impl;

import com.veterinaria.dto.VeterinarioRegistroDTO;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.service.VeterinarioService;
import org.springframework.stereotype.Service;

@Service
public class VeterinarioServiceImpl implements VeterinarioService{
    private final VeterinarioRepository veterinarioRepository;

    public VeterinarioServiceImpl(VeterinarioRepository veterinarioRepository) {
    this.veterinarioRepository = veterinarioRepository;
    }

    @Override
    public Veterinario registrarVeterinario(VeterinarioRegistroDTO dto) {
        // Escenario 3: documento ya existente
        if (veterinarioRepository.existsByDocumentoIdentidad(dto.getDocumentoIdentidad())) {
            throw new IllegalArgumentException("Este veterinario ya se encuentra registrado");
        }

        // Escenario 8: correo ya existente
        if (veterinarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new IllegalArgumentException("Este correo ya se encuentra registrado");
        }

        // Como el Entity usa @Builder (Lombok), armamos el objeto así:
        Veterinario veterinario = Veterinario.builder()
                .nombre(dto.getNombre())
                .documentoIdentidad(dto.getDocumentoIdentidad())
                .telefono(dto.getTelefono())
                .correo(dto.getCorreo())
                .direccion(dto.getDireccion())
                .horarioAtencion(dto.getHorarioAtencion())
                .activo(true) // Escenario 1: estado activo
                .build();

        return veterinarioRepository.save(veterinario);
    }
    
}
