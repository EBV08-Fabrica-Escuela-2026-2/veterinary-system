package com.veterinaria.service;

import com.veterinaria.dto.DisponibilidadDiaDTO;
import com.veterinaria.model.Cita;
import com.veterinaria.model.HorarioAtencion;
import com.veterinaria.model.Servicio;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.CitaRepository;
import com.veterinaria.repository.HorarioAtencionRepository;
import com.veterinaria.repository.ServicioRepository;
import com.veterinaria.service.impl.DisponibilidadServiceImpl;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceImplTest {

    @Mock
    private HorarioAtencionRepository horarioAtencionRepository;

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private DisponibilidadServiceImpl disponibilidadService;

    @Test
    void consultarDisponibilidad_excluyeLaCitaPropiaYConservaLosOtrosSlotsLibres() {
        LocalDate fecha = LocalDate.of(2026, 10, 22);
        Veterinario veterinario = Veterinario.builder().id(4L).nombre("Dra. Laura").build();
        Servicio servicio = Servicio.builder().id(12L).nombre("Consulta").duracionMinutos(30)
                .veterinario(veterinario).build();
        Cita citaPropia = Cita.builder().id(7L).fechaHoraInicio(fecha.atTime(9, 0))
                .fechaHoraFin(fecha.atTime(9, 30)).build();
        HorarioAtencion horario = HorarioAtencion.builder().veterinario(veterinario)
                .diaSemana(DayOfWeek.THURSDAY).horaInicio(java.time.LocalTime.of(9, 0))
                .horaFin(java.time.LocalTime.of(10, 0)).activo(true).build();
        when(servicioRepository.findById(12L)).thenReturn(Optional.of(servicio));
        when(horarioAtencionRepository.findByVeterinarioIdAndDiaSemanaAndActivoTrue(4L, DayOfWeek.THURSDAY))
                .thenReturn(List.of(horario));
        when(citaRepository.findByVeterinarioIdAndEstadoAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
                4L, "CONFIRMADA", fecha.plusDays(1).atStartOfDay(), fecha.atStartOfDay()))
                .thenReturn(List.of(citaPropia));
        when(clock.instant()).thenReturn(Instant.parse("2026-10-15T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        DisponibilidadDiaDTO resultado = disponibilidadService.consultarDisponibilidad(12L, 4L, fecha, 7L);

        assertEquals(2, resultado.getHorariosDisponibles().size());
        assertEquals(java.time.LocalTime.of(9, 0), resultado.getHorariosDisponibles().get(0).getHoraInicio());
        assertEquals(java.time.LocalTime.of(9, 30), resultado.getHorariosDisponibles().get(1).getHoraInicio());
        verify(citaRepository).findByVeterinarioIdAndEstadoAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
                4L, "CONFIRMADA", fecha.plusDays(1).atStartOfDay(), fecha.atStartOfDay());
    }
}