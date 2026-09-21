package com.veterinaria.service.impl;

import com.veterinaria.dto.*;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.Servicio;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.ServicioRepository;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.service.CatalogoService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoServiceImpl implements CatalogoService {

    private final VeterinarioRepository veterinarioRepository;
    private final ServicioRepository servicioRepository;

    @Override
    public CatalogoResponseDTO consultarCatalogo() {
        List<Veterinario> veterinarios = veterinarioRepository.findByActivoTrue();

        if (veterinarios.isEmpty()) {
            return CatalogoResponseDTO.builder()
                    .veterinarios(Collections.emptyList())
                    .moneda("COP")
                    .mensaje("No hay veterinarios disponibles por el momento")
                    .build();
        }

        List<VeterinarioCatalogoDTO> catalogo = veterinarios.stream()
                .map(this::mapToCatalogoDTO)
                .collect(Collectors.toList());

        return CatalogoResponseDTO.builder()
                .veterinarios(catalogo)
                .moneda("COP")
                .build();
    }

    @Override
    public VeterinarioDetalleDTO consultarDetalleVeterinario(Long veterinarioId) {
        Veterinario veterinario = veterinarioRepository.findById(veterinarioId)
                .filter(Veterinario::getActivo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Veterinario no encontrado con id: " + veterinarioId));

        List<Servicio> servicios = servicioRepository
                .findByVeterinarioIdAndActivoTrue(veterinarioId);

        List<ServicioCatalogoDTO> serviciosDTO = servicios.stream()
                .map(this::mapToServicioDTO)
                .collect(Collectors.toList());

        return VeterinarioDetalleDTO.builder()
                .id(veterinario.getId())
                .nombre(veterinario.getNombre())
                .direccion(veterinario.getDireccion())
                .horarioAtencion(veterinario.getHorarioAtencion())
                .telefono(veterinario.getTelefono())
                .servicios(serviciosDTO)
                .moneda("COP")
                .build();
    }

    private VeterinarioCatalogoDTO mapToCatalogoDTO(Veterinario veterinario) {
        List<Servicio> servicios = servicioRepository
                .findByVeterinarioIdAndActivoTrue(veterinario.getId());

        if (servicios.isEmpty()) {
            return VeterinarioCatalogoDTO.builder()
                    .id(veterinario.getId())
                    .nombre(veterinario.getNombre())
                    .direccion(veterinario.getDireccion())
                    .telefono(veterinario.getTelefono())
                    .horarioAtencion(veterinario.getHorarioAtencion())
                    .servicios(Collections.emptyList())
                    .mensajeServicios("Sin servicios publicados")
                    .build();
        }

        List<ServicioCatalogoDTO> serviciosDTO = servicios.stream()
                .map(this::mapToServicioDTO)
                .collect(Collectors.toList());

        return VeterinarioCatalogoDTO.builder()
                .id(veterinario.getId())
                .nombre(veterinario.getNombre())
                .direccion(veterinario.getDireccion())
                .telefono(veterinario.getTelefono())
                .horarioAtencion(veterinario.getHorarioAtencion())
                .servicios(serviciosDTO)
                .build();
    }

    private ServicioCatalogoDTO mapToServicioDTO(Servicio servicio) {
        return ServicioCatalogoDTO.builder()
                .id(servicio.getId())
                .nombre(servicio.getNombre())
                .descripcion(servicio.getDescripcion())
                .precio(servicio.getPrecio().longValue())
                .duracionMinutos(servicio.getDuracionMinutos())
                .build();
    }
}
