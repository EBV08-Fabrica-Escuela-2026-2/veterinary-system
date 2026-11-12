package com.veterinaria.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.dto.OcupacionDistribucionDTO;
import com.veterinaria.dto.OcupacionProfesionalDTO;
import com.veterinaria.dto.OcupacionResumenDTO;
import com.veterinaria.model.Cita;
import com.veterinaria.model.HorarioAtencion;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.CitaRepository;
import com.veterinaria.repository.HorarioAtencionRepository;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.service.OcupacionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OcupacionServiceImpl implements OcupacionService {

    private static final int SLOT_MINUTOS = 30;
    private static final Set<String> ESTADOS_EFECTIVOS = Set.of("CONFIRMADA", "COMPLETADA");

    private final VeterinarioRepository veterinarioRepository;
    private final HorarioAtencionRepository horarioAtencionRepository;
    private final CitaRepository citaRepository;

    @Override
    @Transactional(readOnly = true)
    public OcupacionResumenDTO consultarResumen(LocalDate fechaInicio, LocalDate fechaFin, Long veterinarioId,
            Long sedeId, String agrupacion) {
        validarParametros(fechaInicio, fechaFin, veterinarioId, sedeId);

        AgrupacionOcupacion agrupacionEnum = AgrupacionOcupacion.from(agrupacion);
        List<Veterinario> veterinarios = obtenerVeterinarios(veterinarioId);

        if (veterinarios.isEmpty()) {
            return OcupacionResumenDTO.builder()
                    .fechaInicio(fechaInicio)
                    .fechaFin(fechaFin)
                    .agrupacion(agrupacionEnum.name())
                    .totalSlotsHabilitados(0L)
                    .totalCitasEfectivas(0L)
                    .totalSlotsDisponibles(0L)
                    .porcentajeOcupacion(BigDecimal.ZERO)
                    .distribucion(List.of())
                    .mensaje("No hay registros de turnos para el periodo seleccionado")
                    .build();
        }

        LocalDateTime desde = fechaInicio.atStartOfDay();
        LocalDateTime hasta = fechaFin.plusDays(1).atStartOfDay();
        List<Cita> citasEfectivas = citaRepository.findByFechaHoraInicioGreaterThanEqualAndFechaHoraInicioLessThanAndEstadoIn(
                desde, hasta, ESTADOS_EFECTIVOS);

        List<LocalDate> fechas = new ArrayList<>();
        LocalDate actual = fechaInicio;
        while (!actual.isAfter(fechaFin)) {
            fechas.add(actual);
            actual = actual.plusDays(1);
        }

        Map<String, PeriodBucket> buckets = new LinkedHashMap<>();
        long totalSlots = 0L;
        long totalCitas = 0L;

        for (LocalDate fecha : fechas) {
            for (Veterinario veterinario : veterinarios) {
                long slotsDia = contarSlotsHabilitados(veterinario, fecha);
                totalSlots += slotsDia;
                PeriodBucket bucket = buckets.computeIfAbsent(bucketKey(fecha, agrupacionEnum),
                        key -> PeriodBucket.from(fecha, agrupacionEnum));
                bucket.addSlots(slotsDia);
            }
        }

        for (Cita cita : citasEfectivas) {
            LocalDate fecha = cita.getFechaHoraInicio().toLocalDate();
            if (!fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin) && veterinarios.stream()
                    .anyMatch(veterinario -> Objects.equals(veterinario.getId(), cita.getVeterinario().getId()))) {
                totalCitas++;
                PeriodBucket bucket = buckets.computeIfAbsent(bucketKey(fecha, agrupacionEnum),
                        key -> PeriodBucket.from(fecha, agrupacionEnum));
                bucket.addCitas(1L);
            }
        }

        List<OcupacionDistribucionDTO> distribucion = (totalSlots == 0L && totalCitas == 0L)
                ? List.of()
                : buckets.values().stream()
                        .sorted(Comparator.comparing(PeriodBucket::fechaInicio))
                        .map(PeriodBucket::toDto)
                        .collect(Collectors.toList());

        long slotsDisponibles = Math.max(0L, totalSlots - totalCitas);
        BigDecimal porcentajeOcupacion = totalSlots == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalCitas)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalSlots), 2, RoundingMode.HALF_UP);

        List<OcupacionProfesionalDTO> profesionales = veterinarios.stream()
                .map(veterinario -> construirProfesional(veterinario, fechaInicio, fechaFin))
                .sorted(Comparator.comparing(OcupacionProfesionalDTO::getPorcentajeOcupacion, Comparator.reverseOrder())
                        .thenComparing(OcupacionProfesionalDTO::getCitasEfectivas, Comparator.reverseOrder())
                        .thenComparing(OcupacionProfesionalDTO::getVeterinarioId))
                .collect(Collectors.toList());

        OcupacionProfesionalDTO mayorDemanda = profesionales.isEmpty() ? null : profesionales.get(0);
        OcupacionProfesionalDTO menorDemanda = profesionales.isEmpty() ? null
                : profesionales.stream()
                        .sorted(Comparator.comparing(OcupacionProfesionalDTO::getPorcentajeOcupacion)
                                .thenComparing(OcupacionProfesionalDTO::getCitasEfectivas)
                                .thenComparing(OcupacionProfesionalDTO::getVeterinarioId))
                        .findFirst()
                        .orElse(null);

        String mensaje = (totalSlots == 0L && totalCitas == 0L)
                ? "No hay registros de turnos para el periodo seleccionado"
                : null;

        return OcupacionResumenDTO.builder()
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .agrupacion(agrupacionEnum.name())
                .totalSlotsHabilitados(totalSlots)
                .totalCitasEfectivas(totalCitas)
                .totalSlotsDisponibles(slotsDisponibles)
                .porcentajeOcupacion(porcentajeOcupacion)
                .mayorDemanda(mayorDemanda)
                .menorDemanda(menorDemanda)
                .distribucion(distribucion)
                .mensaje(mensaje)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OcupacionResumenDTO consultarDistribucion(LocalDate fechaInicio, LocalDate fechaFin, Long veterinarioId,
            Long sedeId, String agrupacion) {
        return consultarResumen(fechaInicio, fechaFin, veterinarioId, sedeId, agrupacion);
    }

    private void validarParametros(LocalDate fechaInicio, LocalDate fechaFin, Long veterinarioId, Long sedeId) {
        if (fechaInicio == null) {
            throw new IllegalArgumentException("La fecha inicial es obligatoria");
        }
        if (fechaFin == null) {
            throw new IllegalArgumentException("La fecha final es obligatoria");
        }
        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }
        if (veterinarioId != null && !veterinarioRepository.existsById(veterinarioId)) {
            throw new IllegalArgumentException("El veterinario indicado no existe");
        }
        if (sedeId != null) {
            throw new IllegalArgumentException("El sistema no tiene sedes configuradas para filtrar la ocupación");
        }
    }

    private List<Veterinario> obtenerVeterinarios(Long veterinarioId) {
        if (veterinarioId != null) {
            return veterinarioRepository.findById(veterinarioId)
                    .map(List::of)
                    .orElse(List.of());
        }
        return veterinarioRepository.findByActivoTrue();
    }

    private long contarSlotsHabilitados(Veterinario veterinario, LocalDate fecha) {
        List<HorarioAtencion> bloques = horarioAtencionRepository
                .findByVeterinarioIdAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(veterinario.getId());
        long slots = 0L;
        for (HorarioAtencion bloque : bloques) {
            if (bloque.getDiaSemana() != fecha.getDayOfWeek()) {
                continue;
            }
            if (bloque.getHoraInicio() == null || bloque.getHoraFin() == null) {
                continue;
            }
            long diferenciaMinutos = java.time.Duration.between(bloque.getHoraInicio(), bloque.getHoraFin()).toMinutes();
            if (diferenciaMinutos <= 0) {
                continue;
            }
            slots += Math.max(1L, (long) Math.ceil(diferenciaMinutos / (double) SLOT_MINUTOS));
        }
        return slots;
    }

    private OcupacionProfesionalDTO construirProfesional(Veterinario veterinario, LocalDate fechaInicio, LocalDate fechaFin) {
        long slots = 0L;
        long citas = 0L;
        LocalDate actual = fechaInicio;
        while (!actual.isAfter(fechaFin)) {
            slots += contarSlotsHabilitados(veterinario, actual);
            actual = actual.plusDays(1);
        }

        LocalDateTime desde = fechaInicio.atStartOfDay();
        LocalDateTime hasta = fechaFin.plusDays(1).atStartOfDay();
        List<Cita> citasVendedor = citaRepository.findByFechaHoraInicioGreaterThanEqualAndFechaHoraInicioLessThanAndEstadoIn(
                desde, hasta, ESTADOS_EFECTIVOS);
        citas = citasVendedor.stream()
                .filter(cita -> Objects.equals(cita.getVeterinario().getId(), veterinario.getId()))
                .count();

        long disponibles = Math.max(0L, slots - citas);
        BigDecimal porcentaje = slots == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(citas)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(slots), 2, RoundingMode.HALF_UP);

        return OcupacionProfesionalDTO.builder()
                .veterinarioId(veterinario.getId())
                .nombre(veterinario.getNombre())
                .slotsHabilitados(slots)
                .citasEfectivas(citas)
                .slotsDisponibles(disponibles)
                .porcentajeOcupacion(porcentaje)
                .build();
    }

    private String bucketKey(LocalDate fecha, AgrupacionOcupacion agrupacion) {
        return buildPeriodo(fecha, agrupacion);
    }

    private String buildPeriodo(LocalDate fecha, AgrupacionOcupacion agrupacion) {
        return switch (agrupacion) {
            case DIA -> fecha.toString();
            case SEMANA -> {
                int semana = fecha.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                int anio = fecha.get(IsoFields.WEEK_BASED_YEAR);
                yield anio + "-W" + String.format("%02d", semana);
            }
            case MES -> YearMonth.from(fecha).toString();
        };
    }

    private enum AgrupacionOcupacion {
        DIA,
        SEMANA,
        MES;

        static AgrupacionOcupacion from(String valor) {
            if (valor == null || valor.isBlank()) {
                return DIA;
            }
            try {
                return AgrupacionOcupacion.valueOf(valor.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("La agrupación debe ser DIA, SEMANA o MES");
            }
        }
    }

    private static final class PeriodBucket {
        private final String periodo;
        private final LocalDate fechaInicio;
        private final LocalDate fechaFin;
        private long slots;
        private long citas;

        private PeriodBucket(String periodo, LocalDate fechaInicio, LocalDate fechaFin) {
            this.periodo = periodo;
            this.fechaInicio = fechaInicio;
            this.fechaFin = fechaFin;
        }

        static PeriodBucket from(LocalDate fecha, AgrupacionOcupacion agrupacion) {
            return switch (agrupacion) {
                case DIA -> new PeriodBucket(fecha.toString(), fecha, fecha);
                case SEMANA -> {
                    LocalDate inicioSemana = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    LocalDate finSemana = inicioSemana.plusDays(6);
                    String periodo = inicioSemana.get(IsoFields.WEEK_BASED_YEAR) + "-W"
                            + String.format("%02d", inicioSemana.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
                    yield new PeriodBucket(periodo, inicioSemana, finSemana);
                }
                case MES -> {
                    YearMonth yearMonth = YearMonth.from(fecha);
                    yield new PeriodBucket(yearMonth.toString(), yearMonth.atDay(1), yearMonth.atEndOfMonth());
                }
            };
        }

        void addSlots(long cantidad) {
            this.slots += cantidad;
        }

        void addCitas(long cantidad) {
            this.citas += cantidad;
        }

        LocalDate fechaInicio() {
            return fechaInicio;
        }

        OcupacionDistribucionDTO toDto() {
            long disponibles = Math.max(0L, slots - citas);
            BigDecimal porcentaje = slots == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(citas)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(slots), 2, RoundingMode.HALF_UP);
            return OcupacionDistribucionDTO.builder()
                    .periodo(periodo)
                    .fechaInicio(fechaInicio)
                    .fechaFin(fechaFin)
                    .slotsHabilitados(slots)
                    .citasEfectivas(citas)
                    .slotsDisponibles(disponibles)
                    .porcentajeOcupacion(porcentaje)
                    .build();
        }
    }
}
