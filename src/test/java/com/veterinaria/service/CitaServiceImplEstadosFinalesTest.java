package com.veterinaria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.veterinaria.dto.CancelacionCitaDTO;
import com.veterinaria.dto.EstadoCitaResponseDTO;
import com.veterinaria.exception.CancelacionNoPermitidaException;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.BloqueoHorario;
import com.veterinaria.model.Cita;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.BloqueoHorarioRepository;
import com.veterinaria.repository.CitaRepository;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.repository.MascotaRepository;
import com.veterinaria.repository.ServicioRepository;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.service.impl.CitaServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CitaServiceImplEstadosFinalesTest {

    private static final LocalDateTime FUTURO_INICIO = LocalDateTime.of(2026, 11, 5, 14, 0);
    private static final LocalDateTime FUTURO_FIN = LocalDateTime.of(2026, 11, 5, 14, 30);
    private static final LocalDateTime PASADO_INICIO = LocalDateTime.of(2026, 11, 5, 8, 0);
    private static final LocalDateTime PASADO_FIN = LocalDateTime.of(2026, 11, 5, 8, 45);

    // "Ahora" = 5 de noviembre de 2026, 10:00 (hora de Bogotá)
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-05T15:00:00Z"), ZoneId.of("America/Bogota"));

    @Mock private CitaRepository citaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private MascotaRepository mascotaRepository;
    @Mock private VeterinarioRepository veterinarioRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private DisponibilidadService disponibilidadService;
    @Mock private BloqueoHorarioRepository bloqueoHorarioRepository;

    private CitaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CitaServiceImpl(citaRepository, clienteRepository, mascotaRepository,
                veterinarioRepository, servicioRepository, disponibilidadService, bloqueoHorarioRepository, clock);
    }

    private Veterinario veterinario(Long id, String correo) {
        return Veterinario.builder().id(id).correo(correo).nombre("Dr. Prueba").build();
    }

    private Cita cita(Long id, Veterinario vet, String estado, LocalDateTime inicio, LocalDateTime fin) {
        return Cita.builder().id(id).veterinario(vet).estado(estado)
                .fechaHoraInicio(inicio).fechaHoraFin(fin).build();
    }

    // ---------- cancelar: liberar o bloquear ----------

    @Test
    void cancelar_liberandoCupo_publicaElCupoYNoCreaBloqueo() {
        Veterinario vet = veterinario(1L, "vet@clinica.com");
        Cita cita = cita(10L, vet, "CONFIRMADA", FUTURO_INICIO, FUTURO_FIN);
        when(citaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cita));

        CancelacionCitaDTO r = service.cancelarCitaPorProfesional(
                10L, 1L, "vet@clinica.com", "Urgencia quirúrgica", "LIBERAR_CUPO");

        assertEquals("CANCELADA_PROFESIONAL", r.getEstado());
        assertEquals("LIBERAR_CUPO", r.getDecisionHorario());
        assertTrue(r.isCupoLiberado());
        assertEquals("Urgencia quirúrgica", cita.getMotivoCancelacion());
        assertEquals("vet@clinica.com", cita.getCanceladaPor());
        assertEquals(vet, cita.getCanceladaPorVeterinario());
        verify(citaRepository).save(cita);
        verify(bloqueoHorarioRepository, never()).save(any());
    }

    @Test
    void cancelar_bloqueandoFranja_creaIndisponibilidadYNoPublicaElCupo() {
        Veterinario vet = veterinario(1L, "vet@clinica.com");
        Cita cita = cita(10L, vet, "CONFIRMADA", FUTURO_INICIO, FUTURO_FIN);
        when(citaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cita));

        CancelacionCitaDTO r = service.cancelarCitaPorProfesional(
                10L, 1L, "vet@clinica.com", "Capacitación", "BLOQUEAR_FRANJA");

        assertEquals("CANCELADA_PROFESIONAL", r.getEstado());
        assertEquals("BLOQUEAR_FRANJA", r.getDecisionHorario());
        assertFalse(r.isCupoLiberado());
        ArgumentCaptor<BloqueoHorario> captor = ArgumentCaptor.forClass(BloqueoHorario.class);
        verify(bloqueoHorarioRepository).save(captor.capture());
        BloqueoHorario bloqueo = captor.getValue();
        assertEquals(vet, bloqueo.getVeterinario());
        assertEquals(FUTURO_INICIO, bloqueo.getFechaHoraInicio());
        assertEquals(FUTURO_FIN, bloqueo.getFechaHoraFin());
        assertEquals("Capacitación", bloqueo.getMotivo());
        assertEquals("vet@clinica.com", bloqueo.getCreadoPor());
        assertTrue(bloqueo.getActivo());
    }

    @Test
    void cancelar_sinDecision_liberaElCupoPorDefecto() {
        Veterinario vet = veterinario(1L, "vet@clinica.com");
        Cita cita = cita(10L, vet, "CONFIRMADA", FUTURO_INICIO, FUTURO_FIN);
        when(citaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cita));

        CancelacionCitaDTO r = service.cancelarCitaPorProfesional(10L, 1L, "vet@clinica.com", "Motivo", null);

        assertEquals("LIBERAR_CUPO", r.getDecisionHorario());
        assertTrue(r.isCupoLiberado());
    }

    // ---------- transiciones inválidas: no modifican la cita ----------

    @Test
    void cancelar_citaYaCompletada_noModificaLaCita() {
        Veterinario vet = veterinario(1L, "vet@clinica.com");
        Cita cita = cita(10L, vet, "COMPLETADA", FUTURO_INICIO, FUTURO_FIN);
        when(citaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cita));

        assertThrows(CancelacionNoPermitidaException.class,
                () -> service.cancelarCitaPorProfesional(10L, 1L, "vet@clinica.com", "Motivo", "LIBERAR_CUPO"));

        assertEquals("COMPLETADA", cita.getEstado());
        verify(citaRepository, never()).save(any());
        verify(bloqueoHorarioRepository, never()).save(any());
    }

    @Test
    void cancelar_citaQueYaInicio_esRechazada() {
        Veterinario vet = veterinario(1L, "vet@clinica.com");
        Cita cita = cita(10L, vet, "CONFIRMADA", PASADO_INICIO, PASADO_FIN);
        when(citaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cita));

        assertThrows(CancelacionNoPermitidaException.class,
                () -> service.cancelarCitaPorProfesional(10L, 1L, "vet@clinica.com", "Motivo", "LIBERAR_CUPO"));

        assertEquals("CONFIRMADA", cita.getEstado());
        verify(citaRepository, never()).save(any());
    }

    @Test
    void cancelar_sinMotivo_noConsultaNiModificaNada() {
        assertThrows(IllegalArgumentException.class,
                () -> service.cancelarCitaPorProfesional(10L, 1L, "vet@clinica.com", "   ", "LIBERAR_CUPO"));

        verify(citaRepository, never()).findByIdForUpdate(any());
        verify(citaRepository, never()).save(any());
    }

    @Test
    void cancelar_decisionInvalida_esRechazada() {
        assertThrows(IllegalArgumentException.class,
                () -> service.cancelarCitaPorProfesional(10L, 1L, "vet@clinica.com", "Motivo", "OTRA"));

        verify(citaRepository, never()).save(any());
    }

    // ---------- autorización: propietario o administrador ----------

    @Test
    void cancelar_veterinarioAjeno_noVeLaCitaYNoSeModifica() {
        Veterinario propietario = veterinario(1L, "vet@clinica.com");
        Cita cita = cita(10L, propietario, "CONFIRMADA", FUTURO_INICIO, FUTURO_FIN);
        when(citaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cita));

        assertThrows(ResourceNotFoundException.class,
                () -> service.cancelarCitaPorProfesional(10L, 2L, "otro@clinica.com", "Motivo", "LIBERAR_CUPO"));

        assertEquals("CONFIRMADA", cita.getEstado());
        verify(citaRepository, never()).save(any());
    }

    @Test
    void cancelar_administrador_puedeCancelarCualquierCitaYQuedaComoResponsable() {
        Veterinario propietario = veterinario(1L, "vet@clinica.com");
        Cita cita = cita(10L, propietario, "CONFIRMADA", FUTURO_INICIO, FUTURO_FIN);
        when(citaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cita));

        CancelacionCitaDTO r = service.cancelarCitaPorProfesional(
                10L, null, "admin@clinica.com", "Cierre por mantenimiento", "BLOQUEAR_FRANJA");

        assertEquals("CANCELADA_PROFESIONAL", r.getEstado());
        assertEquals("admin@clinica.com", cita.getCanceladaPor());
        assertNull(cita.getCanceladaPorVeterinario());
        ArgumentCaptor<BloqueoHorario> captor = ArgumentCaptor.forClass(BloqueoHorario.class);
        verify(bloqueoHorarioRepository).save(captor.capture());
        assertEquals("admin@clinica.com", captor.getValue().getCreadoPor());
    }

    // ---------- COMPLETADA / NO_ASISTIO ----------

    @Test
    void cerrar_comoCompletada_despuesDeTerminarElHorario() {
        Cita cita = cita(10L, veterinario(1L, "vet@clinica.com"), "CONFIRMADA", PASADO_INICIO, PASADO_FIN);
        when(citaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cita));

        EstadoCitaResponseDTO r = service.cerrarCitaPorVeterinario(10L, 1L, "COMPLETADA");

        assertEquals("COMPLETADA", r.estado());
        assertEquals("COMPLETADA", cita.getEstado());
        verify(citaRepository).save(cita);
    }

    @Test
    void cerrar_comoNoAsistio_despuesDeTerminarElHorario() {
        Cita cita = cita(10L, veterinario(1L, "vet@clinica.com"), "CONFIRMADA", PASADO_INICIO, PASADO_FIN);
        when(citaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cita));

        EstadoCitaResponseDTO r = service.cerrarCitaPorVeterinario(10L, 1L, "NO_ASISTIO");

        assertEquals("NO_ASISTIO", r.estado());
        assertEquals("NO_ASISTIO", cita.getEstado());
    }

    @Test
    void cerrar_antesDeQueTermineElHorario_esRechazadoYNoModifica() {
        Cita cita = cita(10L, veterinario(1L, "vet@clinica.com"), "CONFIRMADA", FUTURO_INICIO, FUTURO_FIN);
        when(citaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cita));

        assertThrows(CancelacionNoPermitidaException.class,
                () -> service.cerrarCitaPorVeterinario(10L, 1L, "COMPLETADA"));

        assertEquals("CONFIRMADA", cita.getEstado());
        verify(citaRepository, never()).save(any());
    }

    @Test
    void cerrar_citaConResultadoDefinitivo_noPuedeCambiar() {
        Cita cita = cita(10L, veterinario(1L, "vet@clinica.com"), "NO_ASISTIO", PASADO_INICIO, PASADO_FIN);
        when(citaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cita));

        assertThrows(CancelacionNoPermitidaException.class,
                () -> service.cerrarCitaPorVeterinario(10L, 1L, "COMPLETADA"));

        assertEquals("NO_ASISTIO", cita.getEstado());
    }

    @Test
    void cerrar_estadoInvalido_esRechazado() {
        assertThrows(IllegalArgumentException.class,
                () -> service.cerrarCitaPorVeterinario(10L, 1L, "ANULADA"));
    }

    @Test
    void cerrar_veterinarioAjeno_noVeLaCita() {
        Cita cita = cita(10L, veterinario(1L, "vet@clinica.com"), "CONFIRMADA", PASADO_INICIO, PASADO_FIN);
        when(citaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cita));

        assertThrows(ResourceNotFoundException.class,
                () -> service.cerrarCitaPorVeterinario(10L, 2L, "COMPLETADA"));

        assertEquals("CONFIRMADA", cita.getEstado());
    }

    @Test
    void cerrar_administrador_puedeCerrarCualquierCita() {
        Cita cita = cita(10L, veterinario(1L, "vet@clinica.com"), "CONFIRMADA", PASADO_INICIO, PASADO_FIN);
        when(citaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cita));

        assertEquals("COMPLETADA", service.cerrarCitaPorVeterinario(10L, null, "COMPLETADA").estado());
    }

    // ---------- concurrencia (simulación del bloqueo de fila) ----------

    /**
     * Simula el SELECT ... FOR UPDATE de PostgreSQL: la "fila" queda bloqueada desde que se consulta
     * hasta que termina la "transacción" (el finally). Con dos cancelaciones simultáneas de la misma
     * cita solo una tiene éxito y la otra ve la cita ya cancelada.
     */
    @Test
    void cancelar_dosPeticionesSimultaneas_soloUnaTieneExito() throws Exception {
        Veterinario vet = veterinario(1L, "vet@clinica.com");
        Cita cita = cita(10L, vet, "CONFIRMADA", FUTURO_INICIO, FUTURO_FIN);
        ReentrantLock bloqueoDeFila = new ReentrantLock();
        when(citaRepository.findByIdForUpdate(10L)).thenAnswer(invocacion -> {
            bloqueoDeFila.lock();
            return Optional.of(cita);
        });

        int hilos = 2;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            resultados.add(pool.submit(() -> {
                salida.await();
                try {
                    service.cancelarCitaPorProfesional(10L, 1L, "vet@clinica.com", "Urgencia", "BLOQUEAR_FRANJA");
                    return true;
                } catch (CancelacionNoPermitidaException ex) {
                    return false;
                } finally {
                    if (bloqueoDeFila.isHeldByCurrentThread()) {
                        bloqueoDeFila.unlock(); // fin de la "transacción"
                    }
                }
            }));
        }
        salida.countDown();

        int exitos = 0;
        for (Future<Boolean> resultado : resultados) {
            if (resultado.get(5, TimeUnit.SECONDS)) {
                exitos++;
            }
        }
        pool.shutdown();

        assertEquals(1, exitos);
        assertEquals("CANCELADA_PROFESIONAL", cita.getEstado());
        verify(bloqueoHorarioRepository, times(1)).save(any(BloqueoHorario.class));
    }
}
