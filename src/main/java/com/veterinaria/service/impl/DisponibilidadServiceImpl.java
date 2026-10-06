package com.veterinaria.service.impl;

import com.veterinaria.dto.DisponibilidadDiaDTO;
import com.veterinaria.dto.FranjaHorariaDTO;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.Cita;
import com.veterinaria.model.HorarioAtencion;
import com.veterinaria.model.Servicio;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.CitaRepository;
import com.veterinaria.repository.HorarioAtencionRepository;
import com.veterinaria.repository.ServicioRepository;
import com.veterinaria.service.DisponibilidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadServiceImpl implements DisponibilidadService {

    private final HorarioAtencionRepository horarioAtencionRepository;
    private final CitaRepository citaRepository;
    private final ServicioRepository servicioRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadDiaDTO consultarDisponibilidad(Long servicioId, Long veterinarioId, LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }

        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + servicioId));
        if (Boolean.FALSE.equals(servicio.getActivo())) {
            throw new IllegalArgumentException("El servicio no se encuentra activo");
        }

        Veterinario veterinario = servicio.getVeterinario();
        if (veterinarioId != null && !veterinario.getId().equals(veterinarioId)) {
            throw new IllegalArgumentException("El servicio indicado no pertenece al veterinario especificado");
        }

        List<FranjaHorariaDTO> franjas = calcularFranjas(veterinario, servicio.getDuracionMinutos(), fecha);

        DisponibilidadDiaDTO dto = new DisponibilidadDiaDTO();
        dto.setServicioId(servicio.getId());
        dto.setServicioNombre(servicio.getNombre());
        dto.setDuracionMinutos(servicio.getDuracionMinutos());
        dto.setVeterinarioId(veterinario.getId());
        dto.setVeterinarioNombre(veterinario.getNombre());
        dto.setFecha(fecha);
        dto.setHorariosDisponibles(franjas);

        if (franjas.isEmpty()) {
            dto.setHayDisponibilidad(false);
            List<HorarioAtencion> bloques = horarioAtencionRepository
                    .findByVeterinarioIdAndDiaSemanaAndActivoTrue(veterinario.getId(), fecha.getDayOfWeek());
            if (bloques.isEmpty()) {
                dto.setMensaje("El profesional no atiende el día seleccionado.");
            } else {
                dto.setMensaje("No hay horarios disponibles para la fecha seleccionada.");
            }
            dto.setFechaDisponibleMasCercana(buscarFechaMasCercana(veterinario, servicio.getDuracionMinutos(), fecha));
        } else {
            dto.setHayDisponibilidad(true);
            dto.setMensaje(null);
            dto.setFechaDisponibleMasCercana(null);
        }

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisponibilidadDiaDTO> consultarDisponibilidadRango(Long servicioId, Long veterinarioId,
            LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(fechaInicio, fechaFin) > 31) {
            throw new IllegalArgumentException("El rango de consulta no puede superar 31 días");
        }

        List<DisponibilidadDiaDTO> resultado = new ArrayList<>();
        LocalDate dia = fechaInicio;
        while (!dia.isAfter(fechaFin)) {
            resultado.add(consultarDisponibilidad(servicioId, veterinarioId, dia));
            dia = dia.plusDays(1);
        }
        return resultado;
    }

    private List<FranjaHorariaDTO> calcularFranjas(Veterinario veterinario, Integer duracionMinutos, LocalDate fecha) {
        int duracion = (duracionMinutos == null || duracionMinutos <= 0) ? 30 : duracionMinutos;

        List<HorarioAtencion> bloques = horarioAtencionRepository
                .findByVeterinarioIdAndDiaSemanaAndActivoTrue(veterinario.getId(), fecha.getDayOfWeek());
        if (bloques.isEmpty()) {
            return new ArrayList<>();
        }

        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();
        List<Cita> citas = citaRepository
                .findByVeterinarioIdAndEstadoAndFechaHoraInicioGreaterThanEqualAndFechaHoraInicioLessThan(
                        veterinario.getId(), "CONFIRMADA", desde, hasta);

        LocalDateTime ahora = LocalDateTime.now(clock);
        List<FranjaHorariaDTO> franjas = new ArrayList<>();

        for (HorarioAtencion bloque : bloques) {
            LocalDateTime slot = fecha.atTime(bloque.getHoraInicio());
            LocalDateTime finBloque = fecha.atTime(bloque.getHoraFin());

            while (!slot.plusMinutes(duracion).isAfter(finBloque)) {
                LocalDateTime finSlot = slot.plusMinutes(duracion);
                LocalDateTime slotActual = slot;
                boolean futuro = slotActual.isAfter(ahora);
                boolean libre = citas.stream()
                        .noneMatch(cita -> slotActual.isBefore(cita.getFechaHoraFin())
                                && finSlot.isAfter(cita.getFechaHoraInicio()));
                if (futuro && libre) {
                    franjas.add(new FranjaHorariaDTO(slotActual.toLocalTime(), finSlot.toLocalTime()));
                }
                slot = finSlot;
            }
        }

        franjas.sort(Comparator.comparing(FranjaHorariaDTO::getHoraInicio));
        return franjas;
    }

    private LocalDate buscarFechaMasCercana(Veterinario veterinario, Integer duracionMinutos, LocalDate desdeFecha) {
        for (int i = 1; i <= 60; i++) {
            LocalDate candidata = desdeFecha.plusDays(i);
            if (!calcularFranjas(veterinario, duracionMinutos, candidata).isEmpty()) {
                return candidata;
            }
        }
        return null;
    }
}
