package com.veterinaria.service.impl;

import com.veterinaria.dto.ServicioRegistroDTO;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.Servicio;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.ServicioRepository;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.service.ServicioService;
import org.springframework.stereotype.Service;

@Service
public class ServicioServiceImpl implements ServicioService {

    private final ServicioRepository servicioRepository;
    private final VeterinarioRepository veterinarioRepository;

    public ServicioServiceImpl(ServicioRepository servicioRepository, VeterinarioRepository veterinarioRepository) {
        this.servicioRepository = servicioRepository;
        this.veterinarioRepository = veterinarioRepository;
    }

    @Override
    public Servicio registrarServicio(ServicioRegistroDTO dto) {
        // El veterinario debe existir
        Veterinario veterinario = veterinarioRepository.findById(dto.getVeterinarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Veterinario no encontrado con id: " + dto.getVeterinarioId()));

        // El veterinario debe estar activo para poder registrarle servicios
        if (!veterinario.getActivo()) {
            throw new IllegalArgumentException("El veterinario no se encuentra activo");
        }

        Servicio servicio = Servicio.builder()
                .veterinario(veterinario)
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .precio(dto.getPrecio())
                .duracionMinutos(dto.getDuracionMinutos())
                .activo(true)
                .build();

        return servicioRepository.save(servicio);
    }
}
