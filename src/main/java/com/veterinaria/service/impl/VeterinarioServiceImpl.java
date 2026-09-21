package com.veterinaria.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.veterinaria.dto.VeterinarioListaDTO;
import com.veterinaria.dto.VeterinarioRegistroDTO;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.service.VeterinarioService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VeterinarioServiceImpl implements VeterinarioService {

    private final VeterinarioRepository veterinarioRepository;

    @Override
    public Veterinario registrarVeterinario(VeterinarioRegistroDTO dto) {
        String documento = dto.getDocumentoIdentidad().trim();
        String correo = dto.getCorreo().trim();

        if (veterinarioRepository.existsByDocumentoIdentidad(documento)) {
            throw new IllegalArgumentException("Este veterinario ya se encuentra registrado");
        }

        if (veterinarioRepository.existsByCorreo(correo)) {
            throw new IllegalArgumentException("Este correo ya se encuentra registrado");
        }

        Veterinario veterinario = Veterinario.builder()
                .nombre(dto.getNombre().trim())
                .documentoIdentidad(documento)
                .telefono(dto.getTelefono().trim())
                .correo(correo)
                .direccion(normalizarOpcional(dto.getDireccion()))
                .horarioAtencion(normalizarOpcional(dto.getHorarioAtencion()))
                .tipoDocumento(dto.getTipoDocumento().trim())
                .tarjetaProfesional(dto.getTarjetaProfesional().trim())
                .especialidad(dto.getEspecialidad().trim())
                .activo(true)
                .build();

        return veterinarioRepository.save(veterinario);
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    @Override
    public List<VeterinarioListaDTO> listarActivos() {
        return veterinarioRepository.findByActivoTrue()
                .stream()
                .map(v -> new VeterinarioListaDTO(v.getId(), v.getNombre()))
                .collect(Collectors.toList());
    }
}
