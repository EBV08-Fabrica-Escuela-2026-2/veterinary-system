package com.veterinaria.service.impl;

import com.veterinaria.dto.MascotaRegistroDTO;
import com.veterinaria.exception.ClienteNoAutenticadoException;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.Cliente;
import com.veterinaria.model.Mascota;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.repository.MascotaRepository;
import com.veterinaria.service.MascotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MascotaServiceImpl implements MascotaService {

    private final MascotaRepository mascotaRepository;
    private final ClienteRepository clienteRepository;

    @Override
    public Mascota registrarMascota(MascotaRegistroDTO dto, Long clienteId) {
        validarClienteAutenticado(clienteId);
        validarDatosMascota(dto);

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + clienteId));

        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new IllegalArgumentException("El cliente no se encuentra activo");
        }

        Mascota mascota = Mascota.builder()
                .nombre(dto.getNombre().trim())
                .especie(dto.getEspecie().trim())
                .raza(dto.getRaza().trim())
                .edad(dto.getEdad())
                .observaciones(dto.getObservaciones() == null || dto.getObservaciones().isBlank() ? null : dto.getObservaciones().trim())
                .cliente(cliente)
                .build();

        return mascotaRepository.save(mascota);
    }

    @Override
    public List<Mascota> listarMascotasPorCliente(Long clienteId) {
        validarClienteAutenticado(clienteId);
        clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + clienteId));
        return mascotaRepository.findByClienteId(clienteId);
    }

    private void validarClienteAutenticado(Long clienteId) {
        if (clienteId == null) {
            throw new ClienteNoAutenticadoException("Debe enviar el header X-Cliente-Id o el parámetro clienteId para identificar al cliente");
        }
    }

    private void validarDatosMascota(MascotaRegistroDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la mascota son obligatorios");
        }
        if (dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (dto.getEspecie() == null || dto.getEspecie().isBlank()) {
            throw new IllegalArgumentException("La especie es obligatoria");
        }
        if (dto.getRaza() == null || dto.getRaza().isBlank()) {
            throw new IllegalArgumentException("La raza es obligatoria");
        }
        if (dto.getEdad() == null || dto.getEdad() < 0) {
            throw new IllegalArgumentException("La edad ingresada no es válida");
        }
        if (dto.getObservaciones() != null && dto.getObservaciones().length() > 500) {
            throw new IllegalArgumentException("Las observaciones no pueden superar los 500 caracteres");
        }
    }
}
