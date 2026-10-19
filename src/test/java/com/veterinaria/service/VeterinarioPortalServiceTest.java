package com.veterinaria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.veterinaria.dto.DisponibilidadVeterinarioDTO;
import com.veterinaria.dto.FranjaDisponibilidadDTO;
import com.veterinaria.dto.FranjaDisponibilidadRequestDTO;
import com.veterinaria.exception.FranjaTraslapadaException;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.HorarioAtencion;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.CitaRepository;
import com.veterinaria.repository.HorarioAtencionRepository;

@ExtendWith(MockitoExtension.class)
class VeterinarioPortalServiceTest {

    private static final Long VETERINARIO_ID = 1L;
    private static final Long OTRO_VETERINARIO_ID = 2L;
    private static final Long FRANJA_ID = 10L;

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private HorarioAtencionRepository horarioAtencionRepository;

    @Spy
    private ValidadorFranjasHorarias validador = new ValidadorFranjasHorarias();

    @InjectMocks
    private VeterinarioPortalService portalService;

    private final Veterinario veterinario = Veterinario.builder().id(VETERINARIO_ID).nombre("Dr. Camilo").build();

    @Test
    void guardarDisponibilidad_rechazaDosObjetosDelMismoDiaQueSeTraslapan() {
        DisponibilidadVeterinarioDTO solicitud = solicitud(
                dia("LUNES", null, franja("08:00", "10:00")),
                dia("LUNES", null, franja("09:00", "11:00")));

        assertThatThrownBy(() -> portalService.guardarDisponibilidad(veterinario, solicitud))
                .isInstanceOf(FranjaTraslapadaException.class)
                .hasMessage(ValidadorFranjasHorarias.MENSAJE_TRASLAPE);

        verify(horarioAtencionRepository, never()).deleteByVeterinarioId(any());
        verify(horarioAtencionRepository, never()).saveAll(anyList());
    }

    @Test
    void guardarDisponibilidad_aceptaDosObjetosDelMismoDiaConFranjasContiguas() {
        when(horarioAtencionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        DisponibilidadVeterinarioDTO solicitud = solicitud(
                dia("LUNES", null, franja("08:00", "10:00")),
                dia("LUNES", null, franja("10:00", "12:00")));

        DisponibilidadVeterinarioDTO resultado = portalService.guardarDisponibilidad(veterinario, solicitud);

        assertThat(franjasGuardadas()).hasSize(2);
        assertThat(franjasDelDia(resultado, "LUNES")).hasSize(2);
    }

    @Test
    void guardarDisponibilidad_aceptaLaMismaFranjaEnDiasDistintos() {
        when(horarioAtencionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        DisponibilidadVeterinarioDTO solicitud = solicitud(
                dia("LUNES", null, franja("08:00", "10:00")),
                dia("MARTES", null, franja("08:00", "10:00")));

        portalService.guardarDisponibilidad(veterinario, solicitud);

        assertThat(franjasGuardadas()).extracting(HorarioAtencion::getDiaSemana)
                .containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.TUESDAY);
    }

    @Test
    void guardarDisponibilidad_ignoraLasFranjasDeUnDiaInactivo() {
        when(horarioAtencionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        DisponibilidadVeterinarioDTO solicitud = solicitud(
                dia("LUNES", true, franja("08:00", "10:00")),
                dia("MARTES", false, franja("08:00", "10:00")));

        DisponibilidadVeterinarioDTO resultado = portalService.guardarDisponibilidad(veterinario, solicitud);

        assertThat(franjasGuardadas()).extracting(HorarioAtencion::getDiaSemana).containsExactly(DayOfWeek.MONDAY);
        assertThat(franjasDelDia(resultado, "MARTES")).isEmpty();
    }

    @Test
    void guardarDisponibilidad_noValidaLosTraslapesDeUnDiaInactivo() {
        when(horarioAtencionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        DisponibilidadVeterinarioDTO solicitud = solicitud(
                dia("LUNES", true, franja("08:00", "10:00")),
                dia("MARTES", false, franja("08:00", "10:00"), franja("09:00", "11:00")));

        portalService.guardarDisponibilidad(veterinario, solicitud);

        assertThat(franjasGuardadas()).hasSize(1);
    }

    @Test
    void guardarDisponibilidad_rechazaCuandoTodosLosDiasEstanInactivos() {
        DisponibilidadVeterinarioDTO solicitud = solicitud(dia("LUNES", false, franja("08:00", "10:00")));

        assertThatThrownBy(() -> portalService.guardarDisponibilidad(veterinario, solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Selecciona al menos un día y una franja de atención");

        verify(horarioAtencionRepository, never()).deleteByVeterinarioId(any());
    }

    @Test
    void guardarDisponibilidad_rechazaSolicitudSinDiasOSinFranjas() {
        assertThatThrownBy(() -> portalService.guardarDisponibilidad(veterinario, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La disponibilidad es obligatoria");
        assertThatThrownBy(() -> portalService.guardarDisponibilidad(veterinario,
                new DisponibilidadVeterinarioDTO(VETERINARIO_ID, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La disponibilidad es obligatoria");
        assertThatThrownBy(() -> portalService.guardarDisponibilidad(veterinario,
                solicitud(dia("LUNES", null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Selecciona al menos un día y una franja de atención");
    }

    @Test
    void guardarDisponibilidad_rechazaUnRangoInvalidoYNoBorraLoExistente() {
        DisponibilidadVeterinarioDTO solicitud = solicitud(dia("LUNES", null, franja("10:00", "10:00")));

        assertThatThrownBy(() -> portalService.guardarDisponibilidad(veterinario, solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cada franja debe tener una hora final posterior a la inicial");

        verify(horarioAtencionRepository, never()).deleteByVeterinarioId(any());
    }

    @Test
    void guardarDisponibilidad_rechazaUnDiaNoValido() {
        DisponibilidadVeterinarioDTO solicitud = solicitud(dia("FERIADO", null, franja("08:00", "10:00")));

        assertThatThrownBy(() -> portalService.guardarDisponibilidad(veterinario, solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Día de la semana no válido: FERIADO");
    }

    @Test
    void guardarDisponibilidad_traduceLaViolacionDeExclusionDeLaBaseDeDatos() {
        when(horarioAtencionRepository.saveAll(anyList()))
                .thenThrow(new DataIntegrityViolationException("conflicto", new SQLException("exclusion", "23P01")));
        DisponibilidadVeterinarioDTO solicitud = solicitud(dia("LUNES", null, franja("08:00", "10:00")));

        assertThatThrownBy(() -> portalService.guardarDisponibilidad(veterinario, solicitud))
                .isInstanceOf(FranjaTraslapadaException.class);
    }

    @Test
    void crearFranja_guardaLaFranjaYDevuelveSuRepresentacion() {
        when(horarioAtencionRepository.existeTraslape(
                VETERINARIO_ID, DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 0))).thenReturn(false);
        when(horarioAtencionRepository.save(any(HorarioAtencion.class))).thenAnswer(invocation -> {
            HorarioAtencion horario = invocation.getArgument(0);
            horario.setId(FRANJA_ID);
            return horario;
        });

        FranjaDisponibilidadDTO resultado = portalService.crearFranja(veterinario, request("LUNES", "14:00", "16:00"));

        assertThat(resultado).isEqualTo(new FranjaDisponibilidadDTO(
                FRANJA_ID, "LUNES", LocalTime.of(14, 0), LocalTime.of(16, 0)));
        ArgumentCaptor<HorarioAtencion> captor = ArgumentCaptor.forClass(HorarioAtencion.class);
        verify(horarioAtencionRepository).save(captor.capture());
        assertThat(captor.getValue().getVeterinario()).isSameAs(veterinario);
        assertThat(captor.getValue().getActivo()).isTrue();
    }

    @Test
    void crearFranja_rechazaUnTraslapeConUnaFranjaActiva() {
        when(horarioAtencionRepository.existeTraslape(
                VETERINARIO_ID, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0))).thenReturn(true);

        assertThatThrownBy(() -> portalService.crearFranja(veterinario, request("LUNES", "09:00", "11:00")))
                .isInstanceOf(FranjaTraslapadaException.class)
                .hasMessage("La franja se traslapa con otra franja activa del mismo día");

        verify(horarioAtencionRepository, never()).save(any());
    }

    @Test
    void crearFranja_rechazaRangosInvalidosSinConsultarLaBaseDeDatos() {
        assertThatThrownBy(() -> portalService.crearFranja(veterinario, request("LUNES", "10:00", "10:00")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> portalService.crearFranja(veterinario, request("LUNES", "11:00", "10:00")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> portalService.crearFranja(veterinario, request("LUNES", null, "10:00")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> portalService.crearFranja(veterinario, request("LUNES", "08:00", null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(horarioAtencionRepository, never()).existeTraslape(any(), any(), any(), any());
        verify(horarioAtencionRepository, never()).save(any());
    }

    @Test
    void crearFranja_rechazaUnDiaNuloOInvalido() {
        assertThatThrownBy(() -> portalService.crearFranja(veterinario, request(null, "08:00", "09:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El día de la semana es obligatorio");
        assertThatThrownBy(() -> portalService.crearFranja(veterinario, request("FERIADO", "08:00", "09:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Día de la semana no válido: FERIADO");
    }

    @Test
    void crearFranja_traduceLaViolacionDeExclusionPorSolicitudesSimultaneas() {
        when(horarioAtencionRepository.existeTraslape(any(), any(), any(), any())).thenReturn(false);
        when(horarioAtencionRepository.save(any(HorarioAtencion.class)))
                .thenThrow(new DataIntegrityViolationException("conflicto", new SQLException("exclusion", "23P01")));

        assertThatThrownBy(() -> portalService.crearFranja(veterinario, request("LUNES", "08:00", "10:00")))
                .isInstanceOf(FranjaTraslapadaException.class)
                .hasMessage("La franja se traslapa con otra franja activa del mismo día");
    }

    @Test
    void crearFranja_noOcultaOtrasViolacionesDeIntegridad() {
        when(horarioAtencionRepository.existeTraslape(any(), any(), any(), any())).thenReturn(false);
        when(horarioAtencionRepository.save(any(HorarioAtencion.class)))
                .thenThrow(new DataIntegrityViolationException("fk", new SQLException("llave foránea", "23503")));

        assertThatThrownBy(() -> portalService.crearFranja(veterinario, request("LUNES", "08:00", "10:00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void actualizarFranja_permiteEditarUnaFranjaSobreSuPropioRango() {
        HorarioAtencion existente = horario(FRANJA_ID, VETERINARIO_ID, DayOfWeek.MONDAY, "08:00", "10:00", true);
        when(horarioAtencionRepository.findById(FRANJA_ID)).thenReturn(Optional.of(existente));
        when(horarioAtencionRepository.existeTraslapeExcluyendo(
                VETERINARIO_ID, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0), FRANJA_ID))
                .thenReturn(false);
        when(horarioAtencionRepository.save(existente)).thenReturn(existente);

        FranjaDisponibilidadDTO resultado = portalService.actualizarFranja(
                VETERINARIO_ID, FRANJA_ID, request("LUNES", "08:00", "10:00"));

        assertThat(resultado.id()).isEqualTo(FRANJA_ID);
        assertThat(resultado.horaFin()).isEqualTo(LocalTime.of(10, 0));
        verify(horarioAtencionRepository, never()).existeTraslape(any(), any(), any(), any());
    }

    @Test
    void actualizarFranja_actualizaDiaYHorasCuandoNoHayChoques() {
        HorarioAtencion existente = horario(FRANJA_ID, VETERINARIO_ID, DayOfWeek.MONDAY, "08:00", "10:00", true);
        when(horarioAtencionRepository.findById(FRANJA_ID)).thenReturn(Optional.of(existente));
        when(horarioAtencionRepository.existeTraslapeExcluyendo(
                VETERINARIO_ID, DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), FRANJA_ID))
                .thenReturn(false);
        when(horarioAtencionRepository.save(existente)).thenReturn(existente);

        FranjaDisponibilidadDTO resultado = portalService.actualizarFranja(
                VETERINARIO_ID, FRANJA_ID, request("MARTES", "09:00", "12:00"));

        assertThat(resultado).isEqualTo(new FranjaDisponibilidadDTO(
                FRANJA_ID, "MARTES", LocalTime.of(9, 0), LocalTime.of(12, 0)));
        assertThat(existente.getDiaSemana()).isEqualTo(DayOfWeek.TUESDAY);
    }

    @Test
    void actualizarFranja_rechazaUnaEdicionQueChocaConOtraFranja() {
        HorarioAtencion existente = horario(FRANJA_ID, VETERINARIO_ID, DayOfWeek.MONDAY, "08:00", "10:00", true);
        when(horarioAtencionRepository.findById(FRANJA_ID)).thenReturn(Optional.of(existente));
        when(horarioAtencionRepository.existeTraslapeExcluyendo(
                VETERINARIO_ID, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(13, 0), FRANJA_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> portalService.actualizarFranja(
                VETERINARIO_ID, FRANJA_ID, request("LUNES", "08:00", "13:00")))
                .isInstanceOf(FranjaTraslapadaException.class);

        assertThat(existente.getHoraFin()).isEqualTo(LocalTime.of(10, 0));
        verify(horarioAtencionRepository, never()).save(any());
    }

    @Test
    void actualizarFranja_respondeNoEncontradaParaFranjasAjenasInexistentesOInactivas() {
        when(horarioAtencionRepository.findById(FRANJA_ID))
                .thenReturn(Optional.of(horario(FRANJA_ID, OTRO_VETERINARIO_ID, DayOfWeek.MONDAY, "08:00", "10:00", true)));
        assertThatThrownBy(() -> portalService.actualizarFranja(
                VETERINARIO_ID, FRANJA_ID, request("LUNES", "09:00", "10:00")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Franja horaria no encontrada");

        when(horarioAtencionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> portalService.actualizarFranja(
                VETERINARIO_ID, 99L, request("LUNES", "09:00", "10:00")))
                .isInstanceOf(ResourceNotFoundException.class);

        when(horarioAtencionRepository.findById(11L))
                .thenReturn(Optional.of(horario(11L, VETERINARIO_ID, DayOfWeek.MONDAY, "08:00", "10:00", false)));
        assertThatThrownBy(() -> portalService.actualizarFranja(
                VETERINARIO_ID, 11L, request("LUNES", "09:00", "10:00")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(horarioAtencionRepository, never()).save(any());
    }

    @Test
    void eliminarFranja_borraSoloLasFranjasDelVeterinarioAutenticado() {
        HorarioAtencion propia = horario(FRANJA_ID, VETERINARIO_ID, DayOfWeek.MONDAY, "08:00", "10:00", true);
        when(horarioAtencionRepository.findById(FRANJA_ID)).thenReturn(Optional.of(propia));

        portalService.eliminarFranja(VETERINARIO_ID, FRANJA_ID);

        verify(horarioAtencionRepository).delete(propia);
    }

    @Test
    void eliminarFranja_respondeNoEncontradaParaUnaFranjaAjena() {
        HorarioAtencion ajena = horario(FRANJA_ID, OTRO_VETERINARIO_ID, DayOfWeek.MONDAY, "08:00", "10:00", true);
        when(horarioAtencionRepository.findById(FRANJA_ID)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> portalService.eliminarFranja(VETERINARIO_ID, FRANJA_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(horarioAtencionRepository, never()).delete(any(HorarioAtencion.class));
    }

    @Test
    void consultarDisponibilidad_devuelveLosSieteDiasConSusFranjasOrdenadas() {
        when(horarioAtencionRepository.findByVeterinarioIdAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(VETERINARIO_ID))
                .thenReturn(List.of(
                        horario(1L, VETERINARIO_ID, DayOfWeek.MONDAY, "14:00", "16:00", true),
                        horario(2L, VETERINARIO_ID, DayOfWeek.MONDAY, "08:00", "10:00", true),
                        horario(3L, VETERINARIO_ID, DayOfWeek.FRIDAY, "09:00", "12:00", true)));

        DisponibilidadVeterinarioDTO resultado = portalService.consultarDisponibilidad(VETERINARIO_ID);

        assertThat(resultado.dias()).extracting(DisponibilidadVeterinarioDTO.Dia::dia)
                .containsExactly("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO");
        assertThat(franjasDelDia(resultado, "LUNES")).extracting(DisponibilidadVeterinarioDTO.Franja::horaInicio)
                .containsExactly(LocalTime.of(8, 0), LocalTime.of(14, 0));
        assertThat(resultado.dias().get(0).activo()).isTrue();
        assertThat(resultado.dias().get(1).activo()).isFalse();
    }

    private List<HorarioAtencion> franjasGuardadas() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<HorarioAtencion>> captor = ArgumentCaptor.forClass(List.class);
        verify(horarioAtencionRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private List<DisponibilidadVeterinarioDTO.Franja> franjasDelDia(DisponibilidadVeterinarioDTO disponibilidad, String nombre) {
        return disponibilidad.dias().stream()
                .filter(dia -> dia.dia().equals(nombre))
                .findFirst()
                .orElseThrow()
                .franjas();
    }

    private DisponibilidadVeterinarioDTO solicitud(DisponibilidadVeterinarioDTO.Dia... dias) {
        return new DisponibilidadVeterinarioDTO(VETERINARIO_ID, List.of(dias));
    }

    private DisponibilidadVeterinarioDTO.Dia dia(
            String nombre,
            Boolean activo,
            DisponibilidadVeterinarioDTO.Franja... franjas) {
        return new DisponibilidadVeterinarioDTO.Dia(nombre, activo, new ArrayList<>(List.of(franjas)));
    }

    private DisponibilidadVeterinarioDTO.Franja franja(String inicio, String fin) {
        return new DisponibilidadVeterinarioDTO.Franja(null, LocalTime.parse(inicio), LocalTime.parse(fin));
    }

    private FranjaDisponibilidadRequestDTO request(String dia, String inicio, String fin) {
        return new FranjaDisponibilidadRequestDTO(
                dia,
                inicio == null ? null : LocalTime.parse(inicio),
                fin == null ? null : LocalTime.parse(fin));
    }

    private HorarioAtencion horario(
            Long id,
            Long veterinarioId,
            DayOfWeek dia,
            String inicio,
            String fin,
            boolean activo) {
        return HorarioAtencion.builder()
                .id(id)
                .veterinario(Veterinario.builder().id(veterinarioId).build())
                .diaSemana(dia)
                .horaInicio(LocalTime.parse(inicio))
                .horaFin(LocalTime.parse(fin))
                .activo(activo)
                .build();
    }
}
