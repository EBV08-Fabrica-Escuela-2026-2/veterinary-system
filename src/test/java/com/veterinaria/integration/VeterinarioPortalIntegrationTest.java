package com.veterinaria.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veterinaria.model.Cita;
import com.veterinaria.model.Cliente;
import com.veterinaria.model.HorarioAtencion;
import com.veterinaria.model.Mascota;
import com.veterinaria.model.Servicio;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.CitaRepository;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.repository.HorarioAtencionRepository;
import com.veterinaria.repository.MascotaRepository;
import com.veterinaria.repository.ServicioRepository;
import com.veterinaria.repository.VeterinarioRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class VeterinarioPortalIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private VeterinarioRepository veterinarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private HorarioAtencionRepository horarioAtencionRepository;

    @LocalServerPort private int port;

    private Cita cita;
    private Servicio servicio;
    private Veterinario veterinario;

    @BeforeEach
    void prepararDatos() {
        veterinario = veterinarioRepository.findByCorreoIgnoreCase("camilo.duran@vetagenda.com")
                .orElseGet(() -> veterinarioRepository.saveAndFlush(Veterinario.builder()
                        .nombre("Dr. Camilo Durán")
                        .documentoIdentidad("1030000001")
                        .correo("camilo.duran@vetagenda.com")
                        .especialidad("Medicina Interna")
                        .activo(true)
                        .build()));
        String suffix = String.valueOf(System.nanoTime());
        Cliente cliente = clienteRepository.saveAndFlush(Cliente.builder()
                .nombre("Carolina Restrepo")
                .documentoIdentidad(suffix.substring(suffix.length() - 10))
                .telefono("3001234567")
                .correo("carolina-" + suffix + "@example.com")
                .activo(true)
                .build());
        Mascota mascota = mascotaRepository.saveAndFlush(Mascota.builder()
                .nombre("Rocky")
                .especie("Canino")
                .raza("Golden Retriever")
                .edad(4)
                .sexo("Macho")
                .cliente(cliente)
                .build());
        servicio = servicioRepository.saveAndFlush(Servicio.builder()
                .veterinario(veterinario)
                .nombre("Consulta General")
                .precio(BigDecimal.valueOf(85000))
                .duracionMinutos(30)
                .activo(true)
                .build());
        LocalDateTime inicio = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
        cita = citaRepository.saveAndFlush(Cita.builder()
                .cliente(cliente)
                .mascota(mascota)
                .veterinario(veterinario)
                .servicio(servicio)
                .fechaHoraInicio(inicio)
                .fechaHoraFin(inicio.plusMinutes(30))
                .estado("CONFIRMADA")
                .build());
    }

    @Test
    void perfilAgendaCancelacionYDisponibilidadUsanLaSesionVeterinaria() throws Exception {
        assertThat(enviarGet("/api/veterinario-portal/citas", null).statusCode()).isEqualTo(401);

        HttpResponse<String> login = httpClient.send(HttpRequest.newBuilder(uri("/api/auth/veterinario/demo"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(200);
        String token = objectMapper.readTree(login.body()).path("token").asText();
        assertThat(token).isNotBlank();

        JsonNode perfil = objectMapper.readTree(enviarGet("/api/veterinario-portal/perfil", token).body());
        assertThat(perfil.path("correo").asText()).isEqualTo("camilo.duran@vetagenda.com");

        JsonNode agenda = objectMapper.readTree(enviarGet("/api/veterinario-portal/citas", token).body());
        assertThat(agenda.toString()).contains("Carolina Restrepo", "Rocky", "Consulta General");

        HttpResponse<String> horarios = enviarJson("PUT", "/api/veterinario-portal/disponibilidad", Map.of(
                "dias", List.of(Map.of(
                        "dia", "LUNES",
                        "franjas", List.of(Map.of("horaInicio", "08:00", "horaFin", "12:00"))))), token);
        assertThat(horarios.statusCode()).isEqualTo(200);
        assertThat(horarios.body()).contains("LUNES", "08:00:00");

        HttpResponse<String> cancelacion = enviarJson("PATCH",
                "/api/veterinario-portal/citas/" + cita.getId() + "/cancelar",
                Map.of("motivo", "Imprevisto clínico del profesional"), token);
        assertThat(cancelacion.statusCode()).isEqualTo(200);
        assertThat(cancelacion.body()).contains("CANCELADA_PROFESIONAL");
        assertThat(citaRepository.findById(cita.getId()).orElseThrow().getMotivoCancelacion())
                .isEqualTo("Imprevisto clínico del profesional");
    }

    @Test
    void altaDeFranjaDevuelve201AceptaContiguasYRechazaTraslapesConConflicto() throws Exception {
        String token = iniciarSesionVeterinario();
        reemplazarDisponibilidad(token, Map.of("dia", "LUNES", "franjas", List.of(franja("08:00", "12:00"))));

        HttpResponse<String> alta = enviarJson("POST", RUTA_FRANJAS, franjaRequest("LUNES", "14:00", "16:00"), token);
        assertThat(alta.statusCode()).isEqualTo(201);
        JsonNode creada = objectMapper.readTree(alta.body());
        assertThat(creada.path("id").asLong()).isPositive();
        assertThat(creada.path("dia").asText()).isEqualTo("LUNES");
        assertThat(creada.path("horaInicio").asText()).isEqualTo("14:00:00");
        assertThat(creada.path("horaFin").asText()).isEqualTo("16:00:00");

        assertThat(enviarJson("POST", RUTA_FRANJAS, franjaRequest("LUNES", "12:00", "14:00"), token).statusCode())
                .isEqualTo(201);
        assertThat(enviarJson("POST", RUTA_FRANJAS, franjaRequest("MARTES", "14:00", "16:00"), token).statusCode())
                .isEqualTo(201);

        HttpResponse<String> traslape = enviarJson("POST", RUTA_FRANJAS, franjaRequest("LUNES", "15:00", "17:00"), token);
        assertThat(traslape.statusCode()).isEqualTo(409);
        assertThat(objectMapper.readTree(traslape.body()).path("mensaje").asText())
                .isEqualTo("La franja se traslapa con otra franja activa del mismo día");

        JsonNode dias = objectMapper.readTree(enviarGet(RUTA_DISPONIBILIDAD, token).body()).path("dias");
        assertThat(franjasDelDia(dias, "LUNES")).hasSize(3);
        assertThat(franjasDelDia(dias, "MARTES")).hasSize(1);
    }

    @Test
    void edicionDeFranjaPermiteSuPropioRangoYRechazaChoquesYFranjasAjenas() throws Exception {
        String token = iniciarSesionVeterinario();
        reemplazarDisponibilidad(token, Map.of("dia", "LUNES",
                "franjas", List.of(franja("08:00", "12:00"), franja("14:00", "16:00"))));
        JsonNode lunes = franjasDelDia(
                objectMapper.readTree(enviarGet(RUTA_DISPONIBILIDAD, token).body()).path("dias"), "LUNES");
        long idManana = lunes.get(0).path("id").asLong();
        long idTarde = lunes.get(1).path("id").asLong();

        HttpResponse<String> ampliada = enviarJson("PUT", RUTA_FRANJAS + "/" + idManana,
                franjaRequest("LUNES", "08:00", "13:00"), token);
        assertThat(ampliada.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(ampliada.body()).path("horaFin").asText()).isEqualTo("13:00:00");

        assertThat(enviarJson("PUT", RUTA_FRANJAS + "/" + idManana, franjaRequest("LUNES", "08:00", "13:00"), token)
                .statusCode()).isEqualTo(200);
        assertThat(enviarJson("PUT", RUTA_FRANJAS + "/" + idManana, franjaRequest("LUNES", "08:00", "15:00"), token)
                .statusCode()).isEqualTo(409);

        HttpResponse<String> movida = enviarJson("PUT", RUTA_FRANJAS + "/" + idTarde,
                franjaRequest("MARTES", "14:00", "16:00"), token);
        assertThat(movida.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(movida.body()).path("dia").asText()).isEqualTo("MARTES");

        String sufijo = String.valueOf(System.nanoTime());
        Veterinario otro = veterinarioRepository.saveAndFlush(Veterinario.builder()
                .nombre("Dra. Ajena")
                .documentoIdentidad(sufijo.substring(sufijo.length() - 10))
                .correo("ajena-" + sufijo + "@example.com")
                .activo(true)
                .build());
        HorarioAtencion franjaAjena = horarioAtencionRepository.saveAndFlush(HorarioAtencion.builder()
                .veterinario(otro)
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .build());
        assertThat(enviarJson("PUT", RUTA_FRANJAS + "/" + franjaAjena.getId(),
                franjaRequest("LUNES", "09:00", "10:00"), token).statusCode()).isEqualTo(404);
        assertThat(horarioAtencionRepository.findById(franjaAjena.getId()).orElseThrow().getHoraInicio())
                .isEqualTo(LocalTime.of(8, 0));
        assertThat(enviarJson("PUT", RUTA_FRANJAS + "/999999999", franjaRequest("LUNES", "09:00", "10:00"), token)
                .statusCode()).isEqualTo(404);
    }

    @Test
    void lasFranjasExigenSesionDeVeterinario() throws Exception {
        Map<String, Object> cuerpo = franjaRequest("LUNES", "08:00", "09:00");
        HttpResponse<String> sinSesion = enviarJson("POST", RUTA_FRANJAS, cuerpo, null);
        assertThat(sinSesion.statusCode()).isEqualTo(401);
        assertThat(sinSesion.headers().firstValue("Content-Type").orElse("")).containsIgnoringCase("charset=UTF-8");
        assertThat(sinSesion.body()).contains("Se requiere iniciar sesión");
        assertThat(enviarJson("PUT", RUTA_FRANJAS + "/1", cuerpo, null).statusCode()).isEqualTo(401);

        String tokenCliente = iniciarSesionCliente();
        HttpResponse<String> prohibido = enviarJson("POST", RUTA_FRANJAS, cuerpo, tokenCliente);
        assertThat(prohibido.statusCode()).isEqualTo(403);
        assertThat(prohibido.headers().firstValue("Content-Type").orElse("")).containsIgnoringCase("charset=UTF-8");
        assertThat(prohibido.body()).contains("No tienes permisos para esta acción");
        assertThat(enviarJson("PUT", RUTA_FRANJAS + "/1", cuerpo, tokenCliente).statusCode()).isEqualTo(403);
    }

    @Test
    void altaDeFranjaValidaLosDatosDeEntrada() throws Exception {
        String token = iniciarSesionVeterinario();

        assertRechazo(franjaRequest("", "08:00", "09:00"), token, "El día de la semana es obligatorio");
        assertRechazo(franjaRequest("FERIADO", "08:00", "09:00"), token, "Día de la semana no válido: FERIADO");
        assertRechazo(franjaRequest("LUNES", null, "09:00"), token, "La hora de inicio es obligatoria");
        assertRechazo(franjaRequest("LUNES", "08:00", null), token, "La hora de fin es obligatoria");
        assertRechazo(franjaRequest("LUNES", "09:00", "09:00"), token,
                "Cada franja debe tener una hora final posterior a la inicial");
        assertRechazo(franjaRequest("LUNES", "10:00", "09:00"), token,
                "Cada franja debe tener una hora final posterior a la inicial");
        assertRechazo(franjaRequest("LUNES", "25:99", "09:00"), token, "El cuerpo de la solicitud no es válido");
    }

    @Test
    void guardarDisponibilidadAgrupaDiasRepetidosYRespetaDiasInactivos() throws Exception {
        String token = iniciarSesionVeterinario();

        HttpResponse<String> repetidosTraslapados = enviarJson("PUT", RUTA_DISPONIBILIDAD, Map.of("dias", List.of(
                Map.of("dia", "LUNES", "franjas", List.of(franja("08:00", "10:00"))),
                Map.of("dia", "LUNES", "franjas", List.of(franja("09:00", "11:00"))))), token);
        assertThat(repetidosTraslapados.statusCode()).isEqualTo(409);
        assertThat(objectMapper.readTree(repetidosTraslapados.body()).path("mensaje").asText())
                .isEqualTo("Las franjas de un mismo día no pueden traslaparse");

        HttpResponse<String> repetidosContiguos = enviarJson("PUT", RUTA_DISPONIBILIDAD, Map.of("dias", List.of(
                Map.of("dia", "LUNES", "franjas", List.of(franja("08:00", "10:00"))),
                Map.of("dia", "LUNES", "franjas", List.of(franja("10:00", "12:00"))))), token);
        assertThat(repetidosContiguos.statusCode()).isEqualTo(200);
        assertThat(franjasDelDia(objectMapper.readTree(repetidosContiguos.body()).path("dias"), "LUNES")).hasSize(2);

        HttpResponse<String> conDiaInactivo = enviarJson("PUT", RUTA_DISPONIBILIDAD, Map.of("dias", List.of(
                Map.of("dia", "LUNES", "activo", true, "franjas", List.of(franja("08:00", "10:00"))),
                Map.of("dia", "MARTES", "activo", false, "franjas", List.of(franja("08:00", "10:00"))))), token);
        assertThat(conDiaInactivo.statusCode()).isEqualTo(200);
        JsonNode dias = objectMapper.readTree(conDiaInactivo.body()).path("dias");
        assertThat(franjasDelDia(dias, "LUNES")).hasSize(1);
        assertThat(franjasDelDia(dias, "MARTES")).isEmpty();

        HttpResponse<String> soloInactivos = enviarJson("PUT", RUTA_DISPONIBILIDAD, Map.of("dias", List.of(
                Map.of("dia", "LUNES", "activo", false, "franjas", List.of(franja("08:00", "10:00"))))), token);
        assertThat(soloInactivos.statusCode()).isEqualTo(400);

        HttpResponse<String> sinDias = enviarJson("PUT", RUTA_DISPONIBILIDAD, new HashMap<String, Object>(), token);
        assertThat(sinDias.statusCode()).isEqualTo(400);
        assertThat(objectMapper.readTree(sinDias.body()).path("mensaje").asText())
                .isEqualTo("La disponibilidad es obligatoria");
    }

    @Test
    void losCuposPublicosReflejanLasFranjasCreadasPorElVeterinario() throws Exception {
        String token = iniciarSesionVeterinario();
        reemplazarDisponibilidad(token, Map.of("dia", "LUNES", "franjas", List.of(franja("08:00", "10:00"))));
        LocalDate lunes = LocalDate.now().plusDays(7).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        String rutaCupos = "/api/citas/disponibilidad?servicioId=" + servicio.getId()
                + "&veterinarioId=" + veterinario.getId() + "&fecha=" + lunes;

        JsonNode antes = objectMapper.readTree(enviarGet(rutaCupos, null).body());
        assertThat(antes.path("horariosDisponibles")).hasSize(4);

        assertThat(enviarJson("POST", RUTA_FRANJAS, franjaRequest("LUNES", "14:00", "15:00"), token).statusCode())
                .isEqualTo(201);

        JsonNode despues = objectMapper.readTree(enviarGet(rutaCupos, null).body());
        assertThat(despues.path("horariosDisponibles")).hasSize(6);
    }

    private static final String RUTA_DISPONIBILIDAD = "/api/veterinario-portal/disponibilidad";
    private static final String RUTA_FRANJAS = RUTA_DISPONIBILIDAD + "/franjas";

    private String iniciarSesionVeterinario() throws Exception {
        HttpResponse<String> login = httpClient.send(HttpRequest.newBuilder(uri("/api/auth/veterinario/demo"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(200);
        return objectMapper.readTree(login.body()).path("token").asText();
    }

    private String iniciarSesionCliente() throws Exception {
        String sufijo = String.valueOf(System.nanoTime());
        String correo = "cliente-" + sufijo + "@example.com";
        String documento = sufijo.substring(sufijo.length() - 9);
        clienteRepository.saveAndFlush(Cliente.builder()
                .nombre("Cliente sin portal")
                .documentoIdentidad(documento)
                .telefono("3001234567")
                .correo(correo)
                .activo(true)
                .build());
        assertThat(enviarJson("POST", "/api/auth/register", Map.of(
                "correo", correo, "documentoIdentidad", documento, "password", "ClaveSegura123"), null)
                .statusCode()).isEqualTo(201);
        HttpResponse<String> login = enviarJson("POST", "/api/auth/login", Map.of(
                "correo", correo, "password", "ClaveSegura123"), null);
        assertThat(login.statusCode()).isEqualTo(200);
        return objectMapper.readTree(login.body()).path("token").asText();
    }

    private void reemplazarDisponibilidad(String token, Map<String, Object> dia) throws Exception {
        assertThat(enviarJson("PUT", RUTA_DISPONIBILIDAD, Map.of("dias", List.of(dia)), token).statusCode())
                .isEqualTo(200);
    }

    private Map<String, Object> franja(String horaInicio, String horaFin) {
        return Map.of("horaInicio", horaInicio, "horaFin", horaFin);
    }

    private Map<String, Object> franjaRequest(String dia, String horaInicio, String horaFin) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("dia", dia);
        cuerpo.put("horaInicio", horaInicio);
        cuerpo.put("horaFin", horaFin);
        return cuerpo;
    }

    private JsonNode franjasDelDia(JsonNode dias, String nombreDia) {
        for (JsonNode dia : dias) {
            if (nombreDia.equals(dia.path("dia").asText())) {
                return dia.path("franjas");
            }
        }
        throw new AssertionError("No vino el día " + nombreDia);
    }

    private void assertRechazo(Map<String, Object> cuerpo, String token, String mensajeEsperado) throws Exception {
        HttpResponse<String> respuesta = enviarJson("POST", RUTA_FRANJAS, cuerpo, token);
        assertThat(respuesta.statusCode()).as("cuerpo: %s", cuerpo).isEqualTo(400);
        assertThat(objectMapper.readTree(respuesta.body()).path("mensaje").asText()).isEqualTo(mensajeEsperado);
    }

    private HttpResponse<String> enviarGet(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).GET();
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> enviarJson(String method, String path, Object body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
