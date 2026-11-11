package com.veterinaria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veterinaria.dto.CatalogoResponseDTO;
import com.veterinaria.dto.OrientacionRequestDTO;
import com.veterinaria.dto.OrientacionRequestDTO.UnidadDuracion;
import com.veterinaria.dto.OrientacionResponseDTO;
import com.veterinaria.dto.ServicioCatalogoDTO;
import com.veterinaria.dto.VeterinarioCatalogoDTO;
import com.veterinaria.model.Cliente;
import com.veterinaria.model.Mascota;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.repository.MascotaRepository;
import com.veterinaria.service.OrientacionAiClient.Resultado;
import com.veterinaria.service.OrientacionAiClient.ServicioCandidato;
import com.veterinaria.service.OrientacionAiClient.ServicioSeleccionado;
import com.veterinaria.service.impl.OrientacionServiceImpl;

@ExtendWith(MockitoExtension.class)
class OrientacionServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private MascotaRepository mascotaRepository;
    @Mock
    private CatalogoService catalogoService;
    @Mock
    private OrientacionAiClient aiClient;

    private OrientacionServiceImpl orientacionService;

    @BeforeEach
    void setUp() {
        orientacionService = new OrientacionServiceImpl(
                clienteRepository, mascotaRepository, catalogoService, aiClient);

        Cliente cliente = Cliente.builder().id(8L).activo(true).build();
        when(clienteRepository.findByCorreoIgnoreCase("cliente@example.com")).thenReturn(Optional.of(cliente));
    }

    @Test
    void limitaLasRecomendacionesALosServiciosActivosDelCatalogo() {
        configurarMascota();
        when(aiClient.analizar(anyString(), anyInt(), anyString(), anyInt(), anyString(), anyList()))
                .thenReturn(new Resultado(false, List.of(
                        new ServicioSeleccionado(31L, "El servicio contempla valoración de piel."),
                        new ServicioSeleccionado(999L, "Servicio inexistente."))));

        OrientacionResponseDTO response = orientacionService.orientar("cliente@example.com", solicitud("irritación en la piel"));

        assertThat(response.modoRespaldo()).isFalse();
        assertThat(response.recomendaciones()).hasSize(1);
        assertThat(response.recomendaciones().getFirst().servicioId()).isEqualTo(31L);
        assertThat(response.recomendaciones().getFirst().veterinarios())
                .containsExactly(new OrientacionResponseDTO.VeterinarioSugeridoDTO(7L, "Dra. Real"));
        verify(aiClient).analizar(
                eq("Perro"), eq(4), eq("irritación en la piel"), eq(2), eq("dias"), anyList());
    }

    @Test
    void enviaAIAutomaticamenteServiciosNuevosDelCatalogoConSuCategoria() {
        configurarMascota();
        when(catalogoService.consultarCatalogo()).thenReturn(catalogoConServicioAgregado());
        when(aiClient.analizar(anyString(), anyInt(), anyString(), anyInt(), anyString(), anyList()))
                .thenReturn(new Resultado(false, List.of(
                        new ServicioSeleccionado(37L, "El servicio corresponde a los síntomas descritos."))));

        OrientacionResponseDTO response = orientacionService.orientar(
                "cliente@example.com", solicitud("Presenta molestias en los ojos"));

        assertThat(response.recomendaciones())
                .extracting(OrientacionResponseDTO.ServicioRecomendadoDTO::servicioId)
                .containsExactly(37L);
        verify(aiClient).analizar(
                anyString(), anyInt(), anyString(), anyInt(), anyString(),
                argThat(servicios -> servicios.contains(
                        new ServicioCandidato(37L, "Atención ocular", "Evaluación de molestias en los ojos", "Oftalmología"))));
    }

    @Test
    void siLaIaFallaSoloDevuelveCoincidenciasTextualesDelCatalogo() {
        configurarMascota();
        when(aiClient.analizar(anyString(), anyInt(), anyString(), anyInt(), anyString(), anyList()))
                .thenThrow(new OrientacionAiException("Gemini temporalmente no disponible."));

        OrientacionResponseDTO response = orientacionService.orientar(
                "cliente@example.com", solicitud("Presenta irritación en la piel y se rasca"));

        assertThat(response.modoRespaldo()).isTrue();
        assertThat(response.recomendaciones()).extracting(OrientacionResponseDTO.ServicioRecomendadoDTO::servicioId)
                .containsExactly(31L);
        assertThat(response.recomendaciones().getFirst().explicacion()).contains("piel");
    }

    @Test
    void sugiereDermatologiaParaPicazonAunqueGeminiNoDevuelvaOpciones() {
        configurarMascota();
        when(aiClient.analizar(anyString(), anyInt(), anyString(), anyInt(), anyString(), anyList()))
                .thenReturn(new Resultado(false, List.of()));

        OrientacionResponseDTO response = orientacionService.orientar(
                "cliente@example.com", solicitud("Tiene pizacon en la nuca"));

        assertThat(response.modoRespaldo()).isTrue();
        assertThat(response.recomendaciones())
                .extracting(OrientacionResponseDTO.ServicioRecomendadoDTO::servicioId)
                .containsExactly(31L);
        assertThat(response.recomendaciones().getFirst().nombre()).contains("dermatológica");
    }

    @Test
    void sugiereCirugiaParaFracturaPeroNoCirugiaCardiaca() {
        configurarMascota();
        when(aiClient.analizar(anyString(), anyInt(), anyString(), anyInt(), anyString(), anyList()))
                .thenReturn(new Resultado(false, List.of()));

        OrientacionResponseDTO response = orientacionService.orientar(
                "cliente@example.com", solicitud("Posible fractura en una pata tras una caída"));

        assertThat(response.modoRespaldo()).isTrue();
        assertThat(response.recomendaciones())
                .extracting(OrientacionResponseDTO.ServicioRecomendadoDTO::servicioId)
                .containsExactly(33L);
        assertThat(response.recomendaciones().getFirst().nombre()).isEqualTo("Cirugía Mayor");
    }

    @Test
    void sugiereConsultaGeneralParaSintomasSinEspecialidadRelacionada() {
        configurarMascota();
        when(aiClient.analizar(anyString(), anyInt(), anyString(), anyInt(), anyString(), anyList()))
                .thenReturn(new Resultado(false, List.of()));

        OrientacionResponseDTO response = orientacionService.orientar(
                "cliente@example.com", solicitud("Presenta tos persistente"));

        assertThat(response.modoRespaldo()).isTrue();
        assertThat(response.recomendaciones())
                .extracting(OrientacionResponseDTO.ServicioRecomendadoDTO::servicioId)
                .containsExactly(35L);
        assertThat(response.recomendaciones().getFirst().nombre()).isEqualTo("Consulta General");
    }

    @Test
    void sugiereEcografiaYConsultaGeneralParaDolorAbdominalYVomitos() {
        configurarMascota();
        when(aiClient.analizar(anyString(), anyInt(), anyString(), anyInt(), anyString(), anyList()))
                .thenReturn(new Resultado(false, List.of()));

        OrientacionResponseDTO response = orientacionService.orientar(
                "cliente@example.com", solicitud("Tiene dolro abdominal y vomitos"));

        assertThat(response.modoRespaldo()).isTrue();
        assertThat(response.recomendaciones())
                .extracting(OrientacionResponseDTO.ServicioRecomendadoDTO::servicioId)
                .containsExactly(36L, 35L);
        assertThat(response.recomendaciones())
                .extracting(OrientacionResponseDTO.ServicioRecomendadoDTO::nombre)
                .containsExactly("Ecografía", "Consulta General");
    }

    @Test
    void noSugiereServiciosCuandoNoHayCoincidenciasNiConsultaGeneral() {
        configurarMascota();
        when(catalogoService.consultarCatalogo()).thenReturn(catalogoSinConsultaGeneral());
        when(aiClient.analizar(anyString(), anyInt(), anyString(), anyInt(), anyString(), anyList()))
                .thenThrow(new OrientacionAiException("Gemini temporalmente no disponible."));

        OrientacionResponseDTO response = orientacionService.orientar(
                "cliente@example.com", solicitud("Presenta tos persistente"));

        assertThat(response.modoRespaldo()).isTrue();
        assertThat(response.recomendaciones()).isEmpty();
        assertThat(response.mensaje()).contains("No encontramos una relación clara");
    }

    @Test
    void identificaSignosDeAlarmaAunqueLaIaNoLosMarque() {
        configurarMascota();
        when(aiClient.analizar(anyString(), anyInt(), anyString(), anyInt(), anyString(), anyList()))
                .thenReturn(new Resultado(false, List.of()));

        OrientacionResponseDTO response = orientacionService.orientar(
                "cliente@example.com", solicitud("Tiene dificultad para respirar"));

        assertThat(response.urgente()).isTrue();
        assertThat(response.mensajeUrgencia()).contains("atención veterinaria inmediata");
    }

    @Test
    void verificaQueLaMascotaPertenezcaAlClienteAutenticado() {
        when(mascotaRepository.findByIdAndClienteId(19L, 8L)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> orientacionService.orientar("cliente@example.com", solicitud("irritación en la piel")))
                .isInstanceOf(com.veterinaria.exception.ResourceNotFoundException.class);
        verify(aiClient, org.mockito.Mockito.never())
                .analizar(anyString(), anyInt(), anyString(), anyInt(), anyString(), anyList());
    }

    private OrientacionRequestDTO solicitud(String sintomas) {
        return new OrientacionRequestDTO(19L, sintomas, 2, UnidadDuracion.dias);
    }

    private void configurarMascota() {
        Cliente cliente = Cliente.builder().id(8L).activo(true).build();
        Mascota mascota = Mascota.builder().id(19L).cliente(cliente).especie("Perro").edad(4).build();
        when(mascotaRepository.findByIdAndClienteId(19L, 8L)).thenReturn(Optional.of(mascota));
        when(catalogoService.consultarCatalogo()).thenReturn(catalogo());
    }

    private CatalogoResponseDTO catalogo() {
        ServicioCatalogoDTO servicioPiel = ServicioCatalogoDTO.builder()
                .id(31L)
                .nombre("Consulta dermatológica")
                .descripcion("Valoración de piel e irritación en mascotas")
                .categoria("Dermatología")
                .precio(80000L)
                .duracionMinutos(30)
                .build();
        ServicioCatalogoDTO servicioDental = ServicioCatalogoDTO.builder()
                .id(32L)
                .nombre("Limpieza dental")
                .descripcion("Higiene y valoración de dientes")
                .categoria("Odontología")
                .precio(70000L)
                .duracionMinutos(30)
                .build();
        ServicioCatalogoDTO cirugiaMayor = ServicioCatalogoDTO.builder()
                .id(33L)
                .nombre("Cirugía Mayor")
                .descripcion("Procedimiento quirúrgico complejo")
                .categoria("Cirugía")
                .precio(350000L)
                .duracionMinutos(120)
                .build();
        ServicioCatalogoDTO cirugiaCardiaca = ServicioCatalogoDTO.builder()
                .id(34L)
                .nombre("Cirugía Cardíaca")
                .descripcion("Procedimiento cardíaco especializado")
                .categoria("Cardiología")
                .precio(2500000L)
                .duracionMinutos(180)
                .build();
        ServicioCatalogoDTO consultaGeneral = ServicioCatalogoDTO.builder()
                .id(35L)
                .nombre("Consulta General")
                .descripcion("Revisión completa del paciente")
                .categoria("Consulta")
                .precio(85000L)
                .duracionMinutos(30)
                .build();
        ServicioCatalogoDTO ecografia = ServicioCatalogoDTO.builder()
                .id(36L)
                .nombre("Ecografía")
                .descripcion("Examen de ultrasonido")
                .categoria("Diagnóstico por imagen")
                .precio(95000L)
                .duracionMinutos(30)
                .build();
        VeterinarioCatalogoDTO veterinario = VeterinarioCatalogoDTO.builder()
                .id(7L)
                .nombre("Dra. Real")
                .servicios(List.of(
                        servicioPiel, servicioDental, cirugiaMayor, cirugiaCardiaca, consultaGeneral, ecografia))
                .build();
        return CatalogoResponseDTO.builder().veterinarios(List.of(veterinario)).moneda("COP").build();
    }

    private CatalogoResponseDTO catalogoConServicioAgregado() {
        CatalogoResponseDTO catalogo = catalogo();
        VeterinarioCatalogoDTO veterinario = catalogo.getVeterinarios().getFirst();
        ServicioCatalogoDTO servicioAgregado = ServicioCatalogoDTO.builder()
                .id(37L)
                .nombre("Atención ocular")
                .descripcion("Evaluación de molestias en los ojos")
                .categoria("Oftalmología")
                .precio(90000L)
                .duracionMinutos(30)
                .build();
        veterinario.setServicios(new java.util.ArrayList<>(veterinario.getServicios()));
        veterinario.getServicios().add(servicioAgregado);
        return catalogo;
    }

    private CatalogoResponseDTO catalogoSinConsultaGeneral() {
        CatalogoResponseDTO catalogo = catalogo();
        VeterinarioCatalogoDTO veterinario = catalogo.getVeterinarios().getFirst();
        veterinario.setServicios(veterinario.getServicios().stream()
                .filter(servicio -> !servicio.getId().equals(35L))
                .toList());
        return catalogo;
    }
}
