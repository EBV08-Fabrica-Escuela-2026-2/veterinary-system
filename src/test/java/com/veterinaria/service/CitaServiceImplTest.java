package com.veterinaria.service;

import com.veterinaria.dto.CancelacionCitaDTO;
import com.veterinaria.dto.CitaRegistroDTO;
import com.veterinaria.dto.DisponibilidadDiaDTO;
import com.veterinaria.dto.FranjaHorariaDTO;
import com.veterinaria.dto.ReprogramacionCitaResponseDTO;
import com.veterinaria.dto.ReprogramarCitaRequestDTO;
import com.veterinaria.exception.CitaNoDisponibleException;
import com.veterinaria.exception.CancelacionNoPermitidaException;
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
import com.veterinaria.service.impl.CitaServiceImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceImplTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 15, 12, 0);

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private VeterinarioRepository veterinarioRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private DisponibilidadService disponibilidadService;

    @Mock
    private Clock clock;

    @InjectMocks
    private CitaServiceImpl citaService;

    private void configurarReloj() {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-15T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void reprogramarCita_actualizaLaCitaExistenteYConservaSusRelaciones() {
        configurarReloj();
        Cita cita = crearCita(AHORA.plusHours(5));
        when(citaRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(cita));
        when(disponibilidadService.consultarDisponibilidad(12L, 4L, LocalDate.of(2026, 10, 16), 7L))
                .thenReturn(disponibilidadCon(LocalTime.of(9, 0)));

        ReprogramacionCitaResponseDTO resultado = citaService.reprogramarCita(7L, 2L,
                solicitud(LocalDate.of(2026, 10, 16), LocalTime.of(9, 0)));

        assertEquals(AHORA.plusHours(5), resultado.getFechaHoraAnterior());
        assertEquals(LocalDateTime.of(2026, 10, 16, 9, 0), cita.getFechaHoraInicio());
        assertEquals(LocalDateTime.of(2026, 10, 16, 9, 30), cita.getFechaHoraFin());
        assertSame(cita.getCliente(), cita.getMascota().getCliente());
        assertEquals("CONFIRMADA", resultado.getCita().getEstado());
        verify(citaRepository).saveAndFlush(cita);
    }

    @Test
    void reprogramarCita_rechazaCuandoFaltanExactamenteCuatroHoras() {
        configurarReloj();
        Cita cita = crearCita(AHORA.plusHours(4));
        when(citaRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(cita));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> citaService.reprogramarCita(7L, 2L,
                        solicitud(LocalDate.of(2026, 10, 16), LocalTime.of(9, 0))));

        assertEquals("No es posible reprogramar la cita con cuatro horas o menos de antelación. Por políticas de la clínica, debes comunicarte directamente por vía telefónica.",
                exception.getMessage());
        verify(citaRepository, never()).saveAndFlush(any());
        verify(disponibilidadService, never()).consultarDisponibilidad(any(), any(), any(), any());
    }

    @Test
    void consultarDisponibilidad_noRechazaLaConsultaPorFaltarMenosDeCuatroHoras() {
        Cita cita = crearCita(AHORA.plusHours(2));
        LocalDate fechaConsulta = LocalDate.of(2026, 10, 16);
        DisponibilidadDiaDTO disponibilidadEsperada = disponibilidadCon(LocalTime.of(9, 0));
        when(citaRepository.findById(7L)).thenReturn(Optional.of(cita));
        when(disponibilidadService.consultarDisponibilidad(12L, 4L, fechaConsulta, 7L))
                .thenReturn(disponibilidadEsperada);

        DisponibilidadDiaDTO resultado = citaService.consultarDisponibilidadReprogramacion(7L, 2L, fechaConsulta);

        assertSame(disponibilidadEsperada, resultado);
        verify(disponibilidadService).consultarDisponibilidad(12L, 4L, fechaConsulta, 7L);
    }

    @Test
    void reservarCita_rechazaUnaFranjaAnteriorAlRelojConfigurado() {
        configurarReloj();
        Cliente cliente = Cliente.builder().id(2L).build();
        Mascota mascota = Mascota.builder().id(6L).cliente(cliente).build();
        Veterinario veterinario = Veterinario.builder().id(4L).build();
        Servicio servicio = Servicio.builder()
                .id(12L)
                .veterinario(veterinario)
                .duracionMinutos(30)
                .build();
        CitaRegistroDTO solicitud = new CitaRegistroDTO();
        solicitud.setMascotaId(mascota.getId());
        solicitud.setVeterinarioId(veterinario.getId());
        solicitud.setServicioId(servicio.getId());
        solicitud.setFechaHoraInicio(AHORA.minusMinutes(1));

        when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
        when(mascotaRepository.findById(6L)).thenReturn(Optional.of(mascota));
        when(veterinarioRepository.findById(4L)).thenReturn(Optional.of(veterinario));
        when(servicioRepository.findById(12L)).thenReturn(Optional.of(servicio));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> citaService.reservarCita(solicitud, 2L));

        assertEquals("La fecha y hora de la cita deben ser futuras", exception.getMessage());
        verify(citaRepository, never()).save(any());
    }

    @Test
    void reservarCita_rechazaUnaHoraQueNoEstaEnLaDisponibilidadPublicada() {
        configurarReloj();
        Cliente cliente = Cliente.builder().id(2L).build();
        Mascota mascota = Mascota.builder().id(6L).cliente(cliente).build();
        Veterinario veterinario = Veterinario.builder().id(4L).build();
        Servicio servicio = Servicio.builder()
                .id(12L)
                .veterinario(veterinario)
                .duracionMinutos(30)
                .build();
        LocalDateTime inicio = AHORA.plusDays(1).withHour(9).withMinute(0);
        CitaRegistroDTO solicitud = new CitaRegistroDTO();
        solicitud.setMascotaId(mascota.getId());
        solicitud.setVeterinarioId(veterinario.getId());
        solicitud.setServicioId(servicio.getId());
        solicitud.setFechaHoraInicio(inicio);

        when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
        when(mascotaRepository.findById(6L)).thenReturn(Optional.of(mascota));
        when(veterinarioRepository.findById(4L)).thenReturn(Optional.of(veterinario));
        when(servicioRepository.findById(12L)).thenReturn(Optional.of(servicio));
        when(disponibilidadService.consultarDisponibilidad(12L, 4L, inicio.toLocalDate(), null))
                .thenReturn(disponibilidadCon(LocalTime.of(10, 0)));

        CitaNoDisponibleException exception = assertThrows(CitaNoDisponibleException.class,
                () -> citaService.reservarCita(solicitud, 2L));

        assertEquals("El horario seleccionado no está disponible para ese profesional", exception.getMessage());
        verify(citaRepository, never()).save(any());
    }

    @Test
    void reprogramarCita_noRevelaCitasDeOtroCliente() {
        when(citaRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(crearCita(AHORA.plusHours(5))));

        assertThrows(ResourceNotFoundException.class,
                () -> citaService.reprogramarCita(7L, 99L,
                        solicitud(LocalDate.of(2026, 10, 16), LocalTime.of(9, 0))));

        verify(citaRepository, never()).saveAndFlush(any());
    }

    @Test
    void reprogramarCita_rechazaUnaFranjaQueDejoDeEstarDisponible() {
        configurarReloj();
        Cita cita = crearCita(AHORA.plusHours(5));
        when(citaRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(cita));
        when(disponibilidadService.consultarDisponibilidad(12L, 4L, LocalDate.of(2026, 10, 16), 7L))
                .thenReturn(disponibilidadCon(LocalTime.of(10, 0)));

        assertThrows(CitaNoDisponibleException.class,
                () -> citaService.reprogramarCita(7L, 2L,
                        solicitud(LocalDate.of(2026, 10, 16), LocalTime.of(9, 0))));

        verify(citaRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelarCitaPorVeterinario_cancelaLaCitaYLiberaElCupo() {
        configurarReloj();
        Cita cita = crearCita(AHORA.plusHours(5));
        when(citaRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(cita));

        CancelacionCitaDTO resultado = citaService.cancelarCitaPorVeterinario(7L, 4L, "  Emergencia  ");

        assertEquals(CitaServiceImpl.ESTADO_CANCELADA_PROFESIONAL, cita.getEstado());
        assertEquals("Emergencia", cita.getMotivoCancelacion());
        assertSame(cita.getVeterinario(), cita.getCanceladaPorVeterinario());
        assertEquals(AHORA, cita.getCanceladaAt());
        assertEquals(CitaServiceImpl.ESTADO_CANCELADA_PROFESIONAL, resultado.getEstado());
        assertTrue(resultado.isCupoLiberado());
        verify(citaRepository).save(cita);
    }

    @Test
    void cancelarCitaPorVeterinario_noRevelaCitasDeOtroVeterinario() {
        when(citaRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(crearCita(AHORA.plusHours(5))));

        assertThrows(ResourceNotFoundException.class,
                () -> citaService.cancelarCitaPorVeterinario(7L, 99L, "Emergencia"));

        verify(citaRepository, never()).save(any());
    }

    @Test
    void cancelarCitaPorVeterinario_rechazaUnaCitaQueYaNoEstaConfirmada() {
        Cita cita = crearCita(AHORA.plusHours(5));
        cita.setEstado(CitaServiceImpl.ESTADO_ANULADA);
        when(citaRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(cita));

        assertThrows(CancelacionNoPermitidaException.class,
                () -> citaService.cancelarCitaPorVeterinario(7L, 4L, "Emergencia"));

        verify(citaRepository, never()).save(any());
    }

    private Cita crearCita(LocalDateTime inicio) {
        Cliente cliente = Cliente.builder().id(2L).build();
        Mascota mascota = Mascota.builder().id(6L).nombre("Milo").cliente(cliente).build();
        Veterinario veterinario = Veterinario.builder().id(4L).nombre("Dra. Laura").build();
        Servicio servicio = Servicio.builder().id(12L).nombre("Consulta").duracionMinutos(30).build();
        return Cita.builder()
                .id(7L)
                .cliente(cliente)
                .mascota(mascota)
                .veterinario(veterinario)
                .servicio(servicio)
                .fechaHoraInicio(inicio)
                .fechaHoraFin(inicio.plusMinutes(30))
                .estado(CitaServiceImpl.ESTADO_CONFIRMADA)
                .build();
    }

    private ReprogramarCitaRequestDTO solicitud(LocalDate fecha, LocalTime hora) {
        ReprogramarCitaRequestDTO dto = new ReprogramarCitaRequestDTO();
        dto.setNuevaFecha(fecha);
        dto.setNuevaHoraInicio(hora);
        return dto;
    }

    private DisponibilidadDiaDTO disponibilidadCon(LocalTime horaInicio) {
        DisponibilidadDiaDTO dto = new DisponibilidadDiaDTO();
        dto.setHorariosDisponibles(List.of(new FranjaHorariaDTO(horaInicio, horaInicio.plusMinutes(30))));
        return dto;
    }
}
