package com.veterinaria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.veterinaria.dto.DisponibilidadDiaDTO;
import com.veterinaria.dto.FranjaHorariaDTO;
import com.veterinaria.model.BloqueoHorario;
import com.veterinaria.model.HorarioAtencion;
import com.veterinaria.model.Servicio;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.BloqueoHorarioRepository;
import com.veterinaria.repository.CitaRepository;
import com.veterinaria.repository.HorarioAtencionRepository;
import com.veterinaria.repository.ServicioRepository;
import com.veterinaria.service.impl.DisponibilidadServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisponibilidadBloqueosTest {

    private static final LocalDate MARTES = LocalDate.of(2026, 11, 10);

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-05T15:00:00Z"), ZoneId.of("America/Bogota"));

    @Mock private HorarioAtencionRepository horarioAtencionRepository;
    @Mock private CitaRepository citaRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private BloqueoHorarioRepository bloqueoHorarioRepository;

    private DisponibilidadServiceImpl service;
    private Veterinario vet;

    @BeforeEach
    void setUp() {
        service = new DisponibilidadServiceImpl(horarioAtencionRepository, citaRepository,
                servicioRepository, bloqueoHorarioRepository, clock);

        vet = Veterinario.builder().id(10L).nombre("Dr. Prueba").build();
        Servicio servicio = Servicio.builder().id(1L).nombre("Consulta General")
                .veterinario(vet).duracionMinutos(30).build();
        HorarioAtencion horario = HorarioAtencion.builder().veterinario(vet).diaSemana(DayOfWeek.TUESDAY)
                .horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(10, 0)).build();

        when(servicioRepository.findById(1L)).thenReturn(Optional.of(servicio));
        when(horarioAtencionRepository.findByVeterinarioIdAndDiaSemanaAndActivoTrue(10L, DayOfWeek.TUESDAY))
                .thenReturn(List.of(horario));
        when(citaRepository.findByVeterinarioIdAndEstadoAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
                eq(10L), eq("CONFIRMADA"), any(), any())).thenReturn(List.of());
    }

    private List<LocalTime> horasDisponibles() {
        DisponibilidadDiaDTO resultado = service.consultarDisponibilidad(1L, 10L, MARTES);
        return resultado.getHorariosDisponibles().stream().map(FranjaHorariaDTO::getHoraInicio).toList();
    }

    @Test
    void sinBloqueos_publicaTodasLasFranjas() {
        when(bloqueoHorarioRepository
                .findByVeterinarioIdAndActivoTrueAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
                        eq(10L), any(), any())).thenReturn(List.of());

        assertEquals(List.of(LocalTime.of(8, 0), LocalTime.of(8, 30), LocalTime.of(9, 0), LocalTime.of(9, 30)),
                horasDisponibles());
    }

    @Test
    void franjaBloqueada_noSePublicaComoDisponible() {
        BloqueoHorario bloqueo = BloqueoHorario.builder().veterinario(vet)
                .fechaHoraInicio(MARTES.atTime(8, 30)).fechaHoraFin(MARTES.atTime(9, 0))
                .motivo("Urgencia").creadoPor("vet@clinica.com").build();
        when(bloqueoHorarioRepository
                .findByVeterinarioIdAndActivoTrueAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
                        eq(10L), any(), any())).thenReturn(List.of(bloqueo));

        assertEquals(List.of(LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(9, 30)),
                horasDisponibles());
    }
}