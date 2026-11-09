package com.veterinaria.service;

import com.veterinaria.dto.HistorialMascotaDTO;
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
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.service.impl.CitaServiceImpl;
import com.veterinaria.service.impl.MascotaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MascotaHistorialServiceImplTest {

    private static final String CORREO_CLIENTE = "ana@correo.com";
    private static final String CORREO_AJENO = "otro@correo.com";
    private static final String CORREO_VETERINARIO = "laura@correo.com";
    private static final Long MASCOTA_ID = 6L;
    private static final Long CLIENTE_ID = 2L;
    private static final Long VETERINARIO_ID = 4L;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private VeterinarioRepository veterinarioRepository;

    @InjectMocks
    private MascotaServiceImpl mascotaService;

    @Test
    @SuppressWarnings("unchecked")
    void consultarHistorial_clienteTitular_retornaHistorial() {
        Cliente cliente = Cliente.builder().id(CLIENTE_ID).nombre("Ana").correo(CORREO_CLIENTE).activo(true).build();
        Mascota mascota = Mascota.builder().id(MASCOTA_ID).nombre("Milo").cliente(cliente).build();
        List<Cita> citas = List.of(
                cita(1L, CitaServiceImpl.ESTADO_COMPLETADA, LocalDateTime.of(2026, 10, 10, 9, 0)),
                cita(2L, CitaServiceImpl.ESTADO_NO_ASISTIO, LocalDateTime.of(2026, 10, 9, 9, 0)),
                cita(3L, CitaServiceImpl.ESTADO_ANULADA, LocalDateTime.of(2026, 10, 8, 9, 0)),
                cita(4L, CitaServiceImpl.ESTADO_CANCELADA_PROFESIONAL, LocalDateTime.of(2026, 10, 7, 9, 0)));

        when(mascotaRepository.findById(MASCOTA_ID)).thenReturn(Optional.of(mascota));
        when(clienteRepository.findByCorreoIgnoreCase(CORREO_CLIENTE)).thenReturn(Optional.of(cliente));
        when(citaRepository.findByMascotaIdAndEstadoIn(eq(MASCOTA_ID), anyCollection(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(citas, PageRequest.of(0, 10), 4));

        HistorialMascotaDTO resultado = mascotaService.consultarHistorial(MASCOTA_ID, CORREO_CLIENTE, 0, 10);

        assertEquals(MASCOTA_ID, resultado.mascotaId());
        assertEquals("Milo", resultado.mascotaNombre());
        assertEquals(4, resultado.contenido().size());
        assertEquals(0, resultado.pagina());
        assertEquals(10, resultado.limite());
        assertEquals(4L, resultado.totalElementos());
        assertEquals(1, resultado.totalPaginas());
        assertEquals("Completada", resultado.contenido().get(0).etiquetaEstado());
        assertEquals("Cancelada", resultado.contenido().get(3).etiquetaEstado());

        ArgumentCaptor<Collection<String>> estadosCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(citaRepository).findByMascotaIdAndEstadoIn(eq(MASCOTA_ID), estadosCaptor.capture(),
                pageableCaptor.capture());

        assertEquals(Set.of(
                CitaServiceImpl.ESTADO_COMPLETADA,
                CitaServiceImpl.ESTADO_NO_ASISTIO,
                CitaServiceImpl.ESTADO_ANULADA,
                CitaServiceImpl.ESTADO_CANCELADA_PROFESIONAL), new HashSet<>(estadosCaptor.getValue()));

        Sort sort = pageableCaptor.getValue().getSort();
        assertEquals(List.of("fechaHoraInicio", "id"), sort.stream().map(Sort.Order::getProperty).toList());
        assertEquals(Sort.Direction.DESC, sort.getOrderFor("fechaHoraInicio").getDirection());
        assertEquals(Sort.Direction.DESC, sort.getOrderFor("id").getDirection());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(10, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void consultarHistorial_clienteAjeno_lanzaAccessDenied() {
        Cliente titular = Cliente.builder().id(CLIENTE_ID).build();
        Cliente ajeno = Cliente.builder().id(99L).correo(CORREO_AJENO).activo(true).build();
        Mascota mascota = Mascota.builder().id(MASCOTA_ID).nombre("Milo").cliente(titular).build();

        when(mascotaRepository.findById(MASCOTA_ID)).thenReturn(Optional.of(mascota));
        when(clienteRepository.findByCorreoIgnoreCase(CORREO_AJENO)).thenReturn(Optional.of(ajeno));

        assertThrows(AccessDeniedException.class,
                () -> mascotaService.consultarHistorial(MASCOTA_ID, CORREO_AJENO, 0, 10));

        verify(citaRepository, never()).findByMascotaIdAndEstadoIn(any(), any(), any());
        verify(citaRepository, never()).existsByMascotaIdAndVeterinarioId(any(), any());
    }

    @Test
    void consultarHistorial_veterinarioConCitaPrevia_retornaHistorial() {
        Cliente titular = Cliente.builder().id(CLIENTE_ID).build();
        Mascota mascota = Mascota.builder().id(MASCOTA_ID).nombre("Milo").cliente(titular).build();
        Veterinario veterinario = Veterinario.builder().id(VETERINARIO_ID).correo(CORREO_VETERINARIO).activo(true)
                .build();

        when(mascotaRepository.findById(MASCOTA_ID)).thenReturn(Optional.of(mascota));
        when(clienteRepository.findByCorreoIgnoreCase(CORREO_VETERINARIO)).thenReturn(Optional.empty());
        when(veterinarioRepository.findByCorreoIgnoreCase(CORREO_VETERINARIO)).thenReturn(Optional.of(veterinario));
        when(citaRepository.existsByMascotaIdAndVeterinarioId(MASCOTA_ID, VETERINARIO_ID)).thenReturn(true);
        when(citaRepository.findByMascotaIdAndEstadoIn(eq(MASCOTA_ID), anyCollection(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        cita(1L, CitaServiceImpl.ESTADO_COMPLETADA, LocalDateTime.of(2026, 10, 10, 9, 0)))));

        HistorialMascotaDTO resultado = mascotaService.consultarHistorial(MASCOTA_ID, CORREO_VETERINARIO, 0, 10);

        assertEquals(1, resultado.contenido().size());
        verify(citaRepository).existsByMascotaIdAndVeterinarioId(MASCOTA_ID, VETERINARIO_ID);
    }

    @Test
    void consultarHistorial_veterinarioSinCita_lanzaAccessDenied() {
        Cliente titular = Cliente.builder().id(CLIENTE_ID).build();
        Mascota mascota = Mascota.builder().id(MASCOTA_ID).nombre("Milo").cliente(titular).build();
        Veterinario veterinario = Veterinario.builder().id(VETERINARIO_ID).correo(CORREO_VETERINARIO).activo(true)
                .build();

        when(mascotaRepository.findById(MASCOTA_ID)).thenReturn(Optional.of(mascota));
        when(clienteRepository.findByCorreoIgnoreCase(CORREO_VETERINARIO)).thenReturn(Optional.empty());
        when(veterinarioRepository.findByCorreoIgnoreCase(CORREO_VETERINARIO)).thenReturn(Optional.of(veterinario));
        when(citaRepository.existsByMascotaIdAndVeterinarioId(MASCOTA_ID, VETERINARIO_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> mascotaService.consultarHistorial(MASCOTA_ID, CORREO_VETERINARIO, 0, 10));

        verify(citaRepository, never()).findByMascotaIdAndEstadoIn(any(), any(), any());
    }

    @Test
    void consultarHistorial_correoEnAmbasTablasYVeterinarioConCita_retornaHistorial() {
        Cliente titular = Cliente.builder().id(CLIENTE_ID).build();
        Cliente clienteAjeno = Cliente.builder().id(99L).correo(CORREO_VETERINARIO).activo(true).build();
        Mascota mascota = Mascota.builder().id(MASCOTA_ID).nombre("Milo").cliente(titular).build();
        Veterinario veterinario = Veterinario.builder().id(VETERINARIO_ID).correo(CORREO_VETERINARIO).activo(true)
                .build();

        when(mascotaRepository.findById(MASCOTA_ID)).thenReturn(Optional.of(mascota));
        when(clienteRepository.findByCorreoIgnoreCase(CORREO_VETERINARIO)).thenReturn(Optional.of(clienteAjeno));
        when(veterinarioRepository.findByCorreoIgnoreCase(CORREO_VETERINARIO)).thenReturn(Optional.of(veterinario));
        when(citaRepository.existsByMascotaIdAndVeterinarioId(MASCOTA_ID, VETERINARIO_ID)).thenReturn(true);
        when(citaRepository.findByMascotaIdAndEstadoIn(eq(MASCOTA_ID), anyCollection(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        cita(1L, CitaServiceImpl.ESTADO_COMPLETADA, LocalDateTime.of(2026, 10, 10, 9, 0)))));

        HistorialMascotaDTO resultado = mascotaService.consultarHistorial(MASCOTA_ID, CORREO_VETERINARIO, 0, 10);

        assertEquals(1, resultado.contenido().size());
        verify(citaRepository).existsByMascotaIdAndVeterinarioId(MASCOTA_ID, VETERINARIO_ID);
    }

    @Test
    void consultarHistorial_mascotaInexistente_lanzaResourceNotFound() {
        when(mascotaRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> mascotaService.consultarHistorial(99L, CORREO_CLIENTE, 0, 10));

        assertEquals("Mascota no encontrada con id: 99", exception.getMessage());
    }

    @Test
    void consultarHistorial_parametrosInvalidos_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> mascotaService.consultarHistorial(MASCOTA_ID, CORREO_CLIENTE, -1, 10));
        assertThrows(IllegalArgumentException.class,
                () -> mascotaService.consultarHistorial(MASCOTA_ID, CORREO_CLIENTE, 0, 101));
    }

    @Test
    void consultarHistorial_sinAutenticacion_lanzaClienteNoAutenticado() {
        assertThrows(ClienteNoAutenticadoException.class,
                () -> mascotaService.consultarHistorial(MASCOTA_ID, null, 0, 10));
        assertThrows(ClienteNoAutenticadoException.class,
                () -> mascotaService.consultarHistorial(MASCOTA_ID, "   ", 0, 10));
        assertThrows(ClienteNoAutenticadoException.class,
                () -> mascotaService.consultarHistorial(MASCOTA_ID, "anonymousUser", 0, 10));
    }

    private Cita cita(Long id, String estado, LocalDateTime inicio) {
        Cliente cliente = Cliente.builder().id(CLIENTE_ID).build();
        Mascota mascota = Mascota.builder().id(MASCOTA_ID).nombre("Milo").cliente(cliente).build();
        Veterinario veterinario = Veterinario.builder()
                .id(VETERINARIO_ID)
                .nombre("Dra. Laura")
                .especialidad("Medicina general")
                .build();
        Servicio servicio = Servicio.builder()
                .id(12L)
                .nombre("Consulta")
                .categoria("Consulta general")
                .build();
        return Cita.builder()
                .id(id)
                .cliente(cliente)
                .mascota(mascota)
                .veterinario(veterinario)
                .servicio(servicio)
                .fechaHoraInicio(inicio)
                .fechaHoraFin(inicio.plusMinutes(30))
                .estado(estado)
                .diagnostico("Diagnóstico de prueba")
                .tratamiento("Tratamiento de prueba")
                .notasClinicas("Notas de prueba")
                .build();
    }
}
