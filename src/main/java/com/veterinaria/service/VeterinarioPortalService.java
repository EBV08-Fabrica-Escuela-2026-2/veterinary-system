package com.veterinaria.service;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.dto.CitaVeterinarioDTO;
import com.veterinaria.dto.DisponibilidadVeterinarioDTO;
import com.veterinaria.dto.FranjaDisponibilidadDTO;
import com.veterinaria.dto.FranjaDisponibilidadRequestDTO;
import com.veterinaria.exception.FranjaTraslapadaException;
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

    private static final String MENSAJE_FRANJA_NO_ENCONTRADA = "Franja horaria no encontrada";
    private static final String MENSAJE_TRASLAPE_CON_FRANJA_ACTIVA =
            "La franja se traslapa con otra franja activa del mismo día";
    private static final String SQLSTATE_VIOLACION_DE_EXCLUSION = "23P01";

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

        Map<DayOfWeek, List<FranjaHoraria>> franjasPorDia = new EnumMap<>(DayOfWeek.class);
        for (DisponibilidadVeterinarioDTO.Dia dia : solicitud.dias()) {
            if (dia == null || Boolean.FALSE.equals(dia.activo()) || dia.franjas() == null || dia.franjas().isEmpty()) {
                continue;
            }
            franjasPorDia.computeIfAbsent(convertirDia(dia.dia()), clave -> new ArrayList<>())
                    .addAll(dia.franjas().stream()
                            .map(franja -> new FranjaHoraria(franja.horaInicio(), franja.horaFin()))
                            .toList());
        }

        if (franjasPorDia.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un día y una franja de atención");
        }

        List<HorarioAtencion> nuevosHorarios = new ArrayList<>();
        franjasPorDia.forEach((diaSemana, franjas) -> {
            validador.validarSinTraslapes(franjas);
            franjas.forEach(franja -> nuevosHorarios.add(construirHorario(
                    veterinario, diaSemana, franja.horaInicio(), franja.horaFin())));
        });

        horarioAtencionRepository.deleteByVeterinarioId(veterinario.getId());
        horarioAtencionRepository.flush();
        List<HorarioAtencion> guardados = persistir(() -> horarioAtencionRepository.saveAll(nuevosHorarios));
        return construirDisponibilidad(veterinario.getId(), guardados);
    }

    @Transactional
    public FranjaDisponibilidadDTO crearFranja(Veterinario veterinario, FranjaDisponibilidadRequestDTO solicitud) {
        DayOfWeek diaSemana = convertirDia(solicitud.dia());
        validador.validarRango(solicitud.horaInicio(), solicitud.horaFin());
        if (horarioAtencionRepository.existeTraslape(
                veterinario.getId(), diaSemana, solicitud.horaInicio(), solicitud.horaFin())) {
            throw new FranjaTraslapadaException(MENSAJE_TRASLAPE_CON_FRANJA_ACTIVA);
        }

        HorarioAtencion guardado = persistir(() -> horarioAtencionRepository.save(construirHorario(
                veterinario, diaSemana, solicitud.horaInicio(), solicitud.horaFin())));
        return aFranjaDTO(guardado);
    }

    @Transactional
    public FranjaDisponibilidadDTO actualizarFranja(
            Long veterinarioId,
            Long franjaId,
            FranjaDisponibilidadRequestDTO solicitud) {
        HorarioAtencion horario = buscarFranjaDelVeterinario(veterinarioId, franjaId);
        if (!Boolean.TRUE.equals(horario.getActivo())) {
            throw new ResourceNotFoundException(MENSAJE_FRANJA_NO_ENCONTRADA);
        }

        DayOfWeek diaSemana = convertirDia(solicitud.dia());
        validador.validarRango(solicitud.horaInicio(), solicitud.horaFin());
        if (horarioAtencionRepository.existeTraslapeExcluyendo(
                veterinarioId, diaSemana, solicitud.horaInicio(), solicitud.horaFin(), franjaId)) {
            throw new FranjaTraslapadaException(MENSAJE_TRASLAPE_CON_FRANJA_ACTIVA);
        }

        horario.setDiaSemana(diaSemana);
        horario.setHoraInicio(solicitud.horaInicio());
        horario.setHoraFin(solicitud.horaFin());
        return aFranjaDTO(persistir(() -> horarioAtencionRepository.save(horario)));
    }

    @Transactional
    public void eliminarFranja(Long veterinarioId, Long franjaId) {
        horarioAtencionRepository.delete(buscarFranjaDelVeterinario(veterinarioId, franjaId));
    }

    private HorarioAtencion buscarFranjaDelVeterinario(Long veterinarioId, Long franjaId) {
        return horarioAtencionRepository.findById(franjaId)
                .filter(item -> item.getVeterinario().getId().equals(veterinarioId))
                .orElseThrow(() -> new ResourceNotFoundException(MENSAJE_FRANJA_NO_ENCONTRADA));
    }

    private HorarioAtencion construirHorario(
            Veterinario veterinario,
            DayOfWeek diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin) {
        return HorarioAtencion.builder()
                .veterinario(veterinario)
                .diaSemana(diaSemana)
                .horaInicio(horaInicio)
                .horaFin(horaFin)
                .activo(true)
                .build();
    }

    private FranjaDisponibilidadDTO aFranjaDTO(HorarioAtencion horario) {
        return new FranjaDisponibilidadDTO(
                horario.getId(), convertirDia(horario.getDiaSemana()), horario.getHoraInicio(), horario.getHoraFin());
    }

    private <T> T persistir(Supplier<T> operacion) {
        try {
            T resultado = operacion.get();
            horarioAtencionRepository.flush();
            return resultado;
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMostSpecificCause() instanceof SQLException causa
                    && SQLSTATE_VIOLACION_DE_EXCLUSION.equals(causa.getSQLState())) {
                throw new FranjaTraslapadaException(MENSAJE_TRASLAPE_CON_FRANJA_ACTIVA);
            }
            throw ex;
        }
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
