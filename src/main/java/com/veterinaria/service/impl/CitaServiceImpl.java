package com.veterinaria.service.impl;

import com.veterinaria.dto.CancelacionCitaDTO;
import com.veterinaria.dto.CitaClienteDTO;
import com.veterinaria.dto.CitaRegistroDTO;
import com.veterinaria.exception.CancelacionNoPermitidaException;
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

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CitaServiceImpl implements CitaService {

    public static final String ESTADO_CONFIRMADA = "CONFIRMADA";
    public static final String ESTADO_ANULADA = "ANULADA";
    private static final long HORAS_MINIMAS_CANCELACION = 2;

    private final CitaRepository citaRepository;
    private final ClienteRepository clienteRepository;
    private final MascotaRepository mascotaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ServicioRepository servicioRepository;
    private final Clock clock;

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

    @Override
    @Transactional(readOnly = true)
    public List<CitaClienteDTO> listarCitasPorCliente(Long clienteId) {
        if (clienteId == null) {
            throw new ClienteNoAutenticadoException(
                    "Debe enviar el parámetro documentoIdentidad para identificar al cliente");
        }

        List<Cita> citas = citaRepository.findByClienteIdOrderByFechaHoraInicioDesc(clienteId);
        List<CitaClienteDTO> resultado = new ArrayList<>();
        for (Cita cita : citas) {
            resultado.add(toCitaClienteDTO(cita));
        }
        return resultado;
    }

    @Override
    @Transactional
    public CancelacionCitaDTO cancelarCita(Long citaId, Long clienteId) {
        if (clienteId == null) {
            throw new ClienteNoAutenticadoException(
                    "Debe enviar el parámetro documentoIdentidad para identificar al cliente");
        }

        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + citaId));

        if (!cita.getCliente().getId().equals(clienteId)) {
            throw new IllegalArgumentException("La cita no pertenece al cliente especificado");
        }

        if (!ESTADO_CONFIRMADA.equals(cita.getEstado())) {
            throw new CancelacionNoPermitidaException("La cita ya se encuentra anulada o no está activa");
        }

        if (cita.getFechaHoraInicio().isBefore(LocalDateTime.now(clock).plusHours(HORAS_MINIMAS_CANCELACION))) {
            throw new CancelacionNoPermitidaException(
                    "No se puede anular la cita: debe realizarse con al menos 2 horas de anticipación al inicio");
        }

        cita.setEstado(ESTADO_ANULADA);
        citaRepository.save(cita);

        CancelacionCitaDTO dto = new CancelacionCitaDTO();
        dto.setMensaje("Cita anulada exitosamente. El cupo fue liberado.");
        dto.setIdCita(cita.getId());
        dto.setEstado(cita.getEstado());
        dto.setEtiquetaEstado(etiquetaEstado(cita.getEstado()));
        dto.setCupoLiberado(cupoLiberado(cita.getEstado()));
        dto.setFechaHoraInicio(cita.getFechaHoraInicio());
        dto.setFechaHoraFin(cita.getFechaHoraFin());
        return dto;
    }

    private CitaClienteDTO toCitaClienteDTO(Cita cita) {
        CitaClienteDTO dto = new CitaClienteDTO();
        dto.setId(cita.getId());
        dto.setMascotaNombre(cita.getMascota().getNombre());
        dto.setVeterinarioNombre(cita.getVeterinario().getNombre());
        dto.setServicioNombre(cita.getServicio().getNombre());
        dto.setFechaHoraInicio(cita.getFechaHoraInicio());
        dto.setFechaHoraFin(cita.getFechaHoraFin());
        dto.setEstado(cita.getEstado());
        dto.setEtiquetaEstado(etiquetaEstado(cita.getEstado()));
        dto.setCupoLiberado(cupoLiberado(cita.getEstado()));
        dto.setCreatedAt(cita.getCreatedAt());
        return dto;
    }

    private String etiquetaEstado(String estado) {
        if (ESTADO_CONFIRMADA.equals(estado)) {
            return "Activa";
        }
        if (ESTADO_ANULADA.equals(estado)) {
            return "Anulada";
        }
        return estado;
    }

    private boolean cupoLiberado(String estado) {
        return ESTADO_ANULADA.equals(estado);
    }
}