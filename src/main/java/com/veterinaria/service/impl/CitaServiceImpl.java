package com.veterinaria.service.impl;

import com.veterinaria.dto.CitaRegistroDTO;
import com.veterinaria.exception.CitaNoDisponibleException;
import com.veterinaria.exception.ClienteNoAutenticadoException;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.Cita;
import com.veterinaria.model.Cliente;
import com.veterinaria.model.Mascota;
import com.veterinaria.model.Servicio;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.CitaRepository;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.repository.MascotaRepository;
import com.veterinaria.repository.ServicioRepository;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.service.CitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CitaServiceImpl implements CitaService {

    private final CitaRepository citaRepository;
    private final ClienteRepository clienteRepository;
    private final MascotaRepository mascotaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ServicioRepository servicioRepository;

    @Override
    @Transactional
    public Cita reservarCita(CitaRegistroDTO dto, Long clienteId) {
        if (clienteId == null) {
            throw new ClienteNoAutenticadoException(
                    "Debe enviar el parámetro documentoIdentidad para identificar al cliente");
        }

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + clienteId));
        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new IllegalArgumentException("El cliente no se encuentra activo");
        }

        Mascota mascota = mascotaRepository.findById(dto.getMascotaId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Mascota no encontrada con id: " + dto.getMascotaId()));
        if (!mascota.getCliente().getId().equals(clienteId)) {
            throw new IllegalArgumentException("La mascota indicada no pertenece al cliente especificado");
        }

        Veterinario veterinario = veterinarioRepository.findById(dto.getVeterinarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Veterinario no encontrado con id: " + dto.getVeterinarioId()));
        if (Boolean.FALSE.equals(veterinario.getActivo())) {
            throw new IllegalArgumentException("El veterinario no se encuentra activo");
        }

        Servicio servicio = servicioRepository.findById(dto.getServicioId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Servicio no encontrado con id: " + dto.getServicioId()));
        if (Boolean.FALSE.equals(servicio.getActivo())) {
            throw new IllegalArgumentException("El servicio no se encuentra activo");
        }
        if (!servicio.getVeterinario().getId().equals(dto.getVeterinarioId())) {
            throw new IllegalArgumentException("El servicio indicado no pertenece al veterinario especificado");
        }

        LocalDateTime fechaHoraInicio = dto.getFechaHoraInicio();
        if (!fechaHoraInicio.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha y hora de la cita deben ser futuras");
        }
        LocalDateTime fechaHoraFin = fechaHoraInicio.plusMinutes(servicio.getDuracionMinutos());

        Cita cita = Cita.builder()
                .cliente(cliente)
                .mascota(mascota)
                .veterinario(veterinario)
                .servicio(servicio)
                .fechaHoraInicio(fechaHoraInicio)
                .fechaHoraFin(fechaHoraFin)
                .estado("CONFIRMADA")
                .build();

        try {
            return citaRepository.save(cita);
        } catch (DataIntegrityViolationException ex) {
            throw new CitaNoDisponibleException(
                    "El veterinario ya tiene una cita confirmada que se solapa con el horario solicitado");
        }
    }
}