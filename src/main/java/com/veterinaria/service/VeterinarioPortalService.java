package com.veterinaria.service;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.dto.CitaVeterinarioDTO;
import com.veterinaria.dto.DisponibilidadVeterinarioDTO;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.FranjaHoraria;
import com.veterinaria.model.HorarioAtencion;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.CitaRepository;
import com.veterinaria.repository.HorarioAtencionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VeterinarioPortalService {

    private final CitaRepository citaRepository;
    private final HorarioAtencionRepository horarioAtencionRepository;
    private final ValidadorFranjasHorarias validador;

    @Transactional(readOnly = true)
    public List<CitaVeterinarioDTO> listarCitas(Long veterinarioId) {
        return citaRepository.findByVeterinarioIdOrderByFechaHoraInicioDesc(veterinarioId).stream()
                .map(cita -> new CitaVeterinarioDTO(
                        cita.getId(),
                        cita.getCliente().getNombre(),
                        cita.getMascota().getNombre(),
                        cita.getMascota().getEspecie(),
                        cita.getMascota().getRaza(),
                        cita.getServicio().getNombre(),
                        cita.getFechaHoraInicio(),
                        cita.getFechaHoraFin(),
                        cita.getEstado(),
                        cita.getMotivoCancelacion()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DisponibilidadVeterinarioDTO consultarDisponibilidad(Long veterinarioId) {
        List<HorarioAtencion> horarios = horarioAtencionRepository
                .findByVeterinarioIdAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(veterinarioId);
        return construirDisponibilidad(veterinarioId, horarios);
    }

    @Transactional
    public DisponibilidadVeterinarioDTO guardarDisponibilidad(
            Veterinario veterinario,
            DisponibilidadVeterinarioDTO solicitud) {
        if (solicitud == null || solicitud.dias() == null) {
            throw new IllegalArgumentException("La disponibilidad es obligatoria");
        }

        List<HorarioAtencion> nuevosHorarios = new ArrayList<>();
        for (DisponibilidadVeterinarioDTO.Dia dia : solicitud.dias()) {
            if (dia == null || dia.franjas() == null || dia.franjas().isEmpty()) {
                continue;
            }
            DayOfWeek diaSemana = convertirDia(dia.dia());
            validador.validarSinTraslapes(dia.franjas().stream()
                    .map(franja -> new FranjaHoraria(franja.horaInicio(), franja.horaFin()))
                    .toList());
            for (DisponibilidadVeterinarioDTO.Franja franja : dia.franjas()) {
                nuevosHorarios.add(HorarioAtencion.builder()
                        .veterinario(veterinario)
                        .diaSemana(diaSemana)
                        .horaInicio(franja.horaInicio())
                        .horaFin(franja.horaFin())
                        .activo(true)
                        .build());
            }
        }

        if (nuevosHorarios.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un día y una franja de atención");
        }

        horarioAtencionRepository.deleteByVeterinarioId(veterinario.getId());
        horarioAtencionRepository.flush();
        List<HorarioAtencion> guardados = horarioAtencionRepository.saveAll(nuevosHorarios);
        return construirDisponibilidad(veterinario.getId(), guardados);
    }

    @Transactional
    public void eliminarFranja(Long veterinarioId, Long franjaId) {
        HorarioAtencion horario = horarioAtencionRepository.findById(franjaId)
                .filter(item -> item.getVeterinario().getId().equals(veterinarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Franja horaria no encontrada"));
        horarioAtencionRepository.delete(horario);
    }

    private DisponibilidadVeterinarioDTO construirDisponibilidad(
            Long veterinarioId,
            List<HorarioAtencion> horarios) {
        List<DisponibilidadVeterinarioDTO.Dia> dias = new ArrayList<>();
        for (DayOfWeek diaSemana : DayOfWeek.values()) {
            List<DisponibilidadVeterinarioDTO.Franja> franjas = horarios.stream()
                    .filter(horario -> horario.getDiaSemana() == diaSemana && Boolean.TRUE.equals(horario.getActivo()))
                    .sorted(Comparator.comparing(HorarioAtencion::getHoraInicio))
                    .map(horario -> new DisponibilidadVeterinarioDTO.Franja(
                            horario.getId(), horario.getHoraInicio(), horario.getHoraFin()))
                    .toList();
            dias.add(new DisponibilidadVeterinarioDTO.Dia(convertirDia(diaSemana), !franjas.isEmpty(), franjas));
        }
        return new DisponibilidadVeterinarioDTO(veterinarioId, dias);
    }

    private DayOfWeek convertirDia(String dia) {
        if (dia == null) {
            throw new IllegalArgumentException("El día de la semana es obligatorio");
        }
        return switch (dia.trim().toUpperCase()) {
            case "LUNES" -> DayOfWeek.MONDAY;
            case "MARTES" -> DayOfWeek.TUESDAY;
            case "MIERCOLES", "MIÉRCOLES" -> DayOfWeek.WEDNESDAY;
            case "JUEVES" -> DayOfWeek.THURSDAY;
            case "VIERNES" -> DayOfWeek.FRIDAY;
            case "SABADO", "SÁBADO" -> DayOfWeek.SATURDAY;
            case "DOMINGO" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Día de la semana no válido: " + dia);
        };
    }

    private String convertirDia(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO";
            case SUNDAY -> "DOMINGO";
        };
    }
}
