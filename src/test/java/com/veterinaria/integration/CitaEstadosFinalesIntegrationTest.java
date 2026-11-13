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
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veterinaria.model.BloqueoHorario;
import com.veterinaria.model.Cita;
import com.veterinaria.model.Cliente;
import com.veterinaria.model.Mascota;
import com.veterinaria.model.Rol;
import com.veterinaria.model.Servicio;
import com.veterinaria.model.Usuario;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.BloqueoHorarioRepository;
import com.veterinaria.repository.CitaRepository;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.repository.MascotaRepository;
import com.veterinaria.repository.ServicioRepository;
import com.veterinaria.repository.UsuarioRepository;
import com.veterinaria.repository.VeterinarioRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CitaEstadosFinalesIntegrationTest {

    private static final String CORREO_VETERINARIO = "camilo.duran@vetagenda.com";
    private static final String RUTA_DISPONIBILIDAD = "/api/veterinario-portal/disponibilidad";
    private static final String CLAVE = "ClaveSegura123";
    private static final String MOTIVO = "Motivo suficientemente largo";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private VeterinarioRepository veterinarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private BloqueoHorarioRepository bloqueoHorarioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @LocalServerPort private int port;

    private Veterinario veterinario;
    private Servicio servicio;
    private Cliente cliente;
    private Mascota mascota;

    @BeforeEach
    void prepararDatos() {
        veterinario = veterinarioRepository.findByCorreoIgnoreCase(CORREO_VETERINARIO)
                .orElseGet(() -> veterinarioRepository.saveAndFlush(Veterinario.builder()
                        .nombre("Dr. Camilo Durán")
                        .documentoIdentidad("1030000001")
                        .correo(CORREO_VETERINARIO)
                        .especialidad("Medicina Interna")
                        .activo(true)
                        .build()));
        String sufijo = String.valueOf(System.nanoTime());
        cliente = clienteRepository.saveAndFlush(Cliente.builder()
                .nombre("Cliente de prueba")
                .documentoIdentidad(sufijo.substring(sufijo.length() - 10))
                .telefono("3001234567")
                .correo("cliente-" + sufijo + "@example.com")
                .activo(true)
                .build());
        mascota = mascotaRepository.saveAndFlush(Mascota.builder()
                .nombre("Rocky").especie("Canino").raza("Golden Retriever")
                .edad(4).sexo("Macho").cliente(cliente).build());
        servicio = servicioRepository.saveAndFlush(Servicio.builder()
                .veterinario(veterinario).nombre("Consulta General")
                .precio(BigDecimal.valueOf(85000)).duracionMinutos(30).activo(true).build());
    }

    @Test
    void cancelarLiberandoPublicaElHorarioYBloqueandoLoMantieneOcupado() throws Exception {
        String token = iniciarSesionVeterinario();
        LocalDate lunes = proximoLunes(2);
        publicarLunesDeOchoADoce(token);
        Cita citaA = crearCita(lunes.atTime(9, 0));
        Cita citaB = crearCita(lunes.atTime(10, 0));
        assertThat(horasPublicadas(lunes)).doesNotContain("09:00:00", "10:00:00");

        JsonNode liberada = objectMapper.readTree(
                cancelarPortal(token, citaA.getId(), "Urgencia quirúrgica imprevista", "LIBERAR_CUPO").body());
        assertThat(liberada.path("estado").asText()).isEqualTo("CANCELADA_PROFESIONAL");
        assertThat(liberada.path("decisionHorario").asText()).isEqualTo("LIBERAR_CUPO");
        assertThat(liberada.path("cupoLiberado").asBoolean()).isTrue();

        JsonNode bloqueada = objectMapper.readTree(
                cancelarPortal(token, citaB.getId(), "Capacitación obligatoria del equipo", "BLOQUEAR_FRANJA").body());
        assertThat(bloqueada.path("decisionHorario").asText()).isEqualTo("BLOQUEAR_FRANJA");
        assertThat(bloqueada.path("cupoLiberado").asBoolean()).isFalse();

        assertThat(horasPublicadas(lunes)).contains("09:00:00").doesNotContain("10:00:00");

        Cita guardadaB = citaRepository.findById(citaB.getId()).orElseThrow();
        assertThat(guardadaB.getEstado()).isEqualTo("CANCELADA_PROFESIONAL");
        assertThat(guardadaB.getDecisionHorario()).isEqualTo("BLOQUEAR_FRANJA");
        assertThat(guardadaB.getCanceladaPor()).isEqualTo(CORREO_VETERINARIO);
        assertThat(guardadaB.getCanceladaAt()).isNotNull();

        List<BloqueoHorario> bloqueos = bloqueoHorarioRepository.findByVeterinarioIdAndActivoTrue(veterinario.getId());
        assertThat(bloqueos).anyMatch(b -> b.getFechaHoraInicio().equals(lunes.atTime(10, 0))
                && b.getMotivo().equals("Capacitación obligatoria del equipo")
                && b.getCreadoPor().equals(CORREO_VETERINARIO));
        assertThat(bloqueos).noneMatch(b -> b.getFechaHoraInicio().equals(lunes.atTime(9, 0)));
    }

    @Test
    void elVeterinarioCierraCitasPasadasYLasTransicionesInvalidasNoModificanNada() throws Exception {
        String token = iniciarSesionVeterinario();
        Cita atendida = crearCita(LocalDateTime.now().minusDays(1).withSecond(0).withNano(0));
        Cita inasistencia = crearCita(LocalDateTime.now().minusDays(2).withSecond(0).withNano(0));
        Cita futura = crearCita(proximoLunes(3).atTime(9, 0));

        HttpResponse<String> completada = cambiarEstado(token, atendida.getId(), "COMPLETADA");
        assertThat(completada.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(completada.body()).path("estado").asText()).isEqualTo("COMPLETADA");
        assertThat(estadoGuardado(atendida)).isEqualTo("COMPLETADA");

        assertThat(cambiarEstado(token, inasistencia.getId(), "NO_ASISTIO").statusCode()).isEqualTo(200);
        assertThat(estadoGuardado(inasistencia)).isEqualTo("NO_ASISTIO");

        assertThat(cambiarEstado(token, atendida.getId(), "NO_ASISTIO").statusCode()).isEqualTo(409);
        assertThat(cancelarPortal(token, atendida.getId(), MOTIVO, "LIBERAR_CUPO").statusCode()).isEqualTo(409);
        assertThat(estadoGuardado(atendida)).isEqualTo("COMPLETADA");

        assertThat(cambiarEstado(token, futura.getId(), "COMPLETADA").statusCode()).isEqualTo(409);
        assertThat(cambiarEstado(token, futura.getId(), "ANULADA").statusCode()).isEqualTo(400);
        assertThat(cancelarPortal(token, futura.getId(), "", "LIBERAR_CUPO").statusCode()).isEqualTo(400);
        assertThat(cancelarPortal(token, futura.getId(), MOTIVO, "OTRA").statusCode()).isEqualTo(400);
        assertThat(estadoGuardado(futura)).isEqualTo("CONFIRMADA");

        assertThat(cancelarPortal(token, futura.getId(), MOTIVO, "LIBERAR_CUPO").statusCode()).isEqualTo(200);
        assertThat(cancelarPortal(token, futura.getId(), MOTIVO, "LIBERAR_CUPO").statusCode()).isEqualTo(409);
    }

    @Test
    void soloElVeterinarioOLaAdministracionPuedenGestionarLaCita() throws Exception {
        Cita cita = crearCita(proximoLunes(4).atTime(9, 0));
        Cita pasada = crearCita(LocalDateTime.now().minusDays(3).withSecond(0).withNano(0));
        Map<String, Object> cuerpo = cuerpoCancelacion(MOTIVO, "LIBERAR_CUPO");
        String rutaAdmin = "/api/admin/citas/" + cita.getId() + "/cancelar";

        assertThat(enviarJson("PATCH", rutaPortal(cita), cuerpo, null).statusCode()).isEqualTo(401);

        String tokenCliente = iniciarSesionCliente();
        assertThat(enviarJson("PATCH", rutaPortal(cita), cuerpo, tokenCliente).statusCode()).isEqualTo(403);
        assertThat(enviarJson("PATCH", rutaAdmin, cuerpo, tokenCliente).statusCode()).isEqualTo(403);

        String tokenVeterinario = iniciarSesionVeterinario();
        assertThat(enviarJson("PATCH", rutaAdmin, cuerpo, tokenVeterinario).statusCode()).isEqualTo(403);
        assertThat(estadoGuardado(cita)).isEqualTo("CONFIRMADA");

        String tokenAdmin = iniciarSesionAdministrador();
        assertThat(enviarJson("PATCH", rutaAdmin, cuerpo, tokenAdmin).statusCode()).isEqualTo(200);
        Cita guardada = citaRepository.findById(cita.getId()).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo("CANCELADA_PROFESIONAL");
        assertThat(guardada.getCanceladaPor()).startsWith("admin-");

        HttpResponse<String> cierre = enviarJson("PATCH", "/api/admin/citas/" + pasada.getId() + "/estado",
                Map.of("estado", "NO_ASISTIO"), tokenAdmin);
        assertThat(cierre.statusCode()).isEqualTo(200);
        assertThat(estadoGuardado(pasada)).isEqualTo("NO_ASISTIO");
    }

    // ---------- utilidades ----------

    private LocalDate proximoLunes(int semanas) {
        return LocalDate.now().plusWeeks(semanas).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private Cita crearCita(LocalDateTime inicio) {
        return citaRepository.saveAndFlush(Cita.builder()
                .cliente(cliente).mascota(mascota).veterinario(veterinario).servicio(servicio)
                .fechaHoraInicio(inicio).fechaHoraFin(inicio.plusMinutes(30))
                .estado("CONFIRMADA").build());
    }

    private String estadoGuardado(Cita cita) {
        return citaRepository.findById(cita.getId()).orElseThrow().getEstado();
    }

    private String rutaPortal(Cita cita) {
        return "/api/veterinario-portal/citas/" + cita.getId() + "/cancelar";
    }

    private Map<String, Object> cuerpoCancelacion(String motivo, String decision) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("motivo", motivo);
        if (decision != null) {
            cuerpo.put("decisionHorario", decision);
        }
        return cuerpo;
    }

    private HttpResponse<String> cancelarPortal(String token, Long idCita, String motivo, String decision)
            throws Exception {
        return enviarJson("PATCH", "/api/veterinario-portal/citas/" + idCita + "/cancelar",
                cuerpoCancelacion(motivo, decision), token);
    }

    private HttpResponse<String> cambiarEstado(String token, Long idCita, String estado) throws Exception {
        return enviarJson("PATCH", "/api/veterinario-portal/citas/" + idCita + "/estado",
                Map.of("estado", estado), token);
    }

    private void publicarLunesDeOchoADoce(String token) throws Exception {
        HttpResponse<String> respuesta = enviarJson("PUT", RUTA_DISPONIBILIDAD, Map.of("dias", List.of(
                Map.of("dia", "LUNES", "franjas", List.of(Map.of("horaInicio", "08:00", "horaFin", "12:00"))))), token);
        assertThat(respuesta.statusCode()).isEqualTo(200);
    }

    private List<String> horasPublicadas(LocalDate dia) throws Exception {
        String ruta = "/api/citas/disponibilidad?servicioId=" + servicio.getId()
                + "&veterinarioId=" + veterinario.getId() + "&fecha=" + dia;
        JsonNode franjas = objectMapper.readTree(enviarGet(ruta, null).body()).path("horariosDisponibles");
        List<String> horas = new ArrayList<>();
        franjas.forEach(franja -> horas.add(franja.path("horaInicio").asText()));
        return horas;
    }

    private String iniciarSesionVeterinario() throws Exception {
        HttpResponse<String> login = httpClient.send(HttpRequest.newBuilder(uri("/api/auth/veterinario/demo"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(200);
        return objectMapper.readTree(login.body()).path("token").asText();
    }

    private String iniciarSesionCliente() throws Exception {
        assertThat(enviarJson("POST", "/api/auth/register", Map.of(
                "correo", cliente.getCorreo(), "documentoIdentidad", cliente.getDocumentoIdentidad(),
                "password", CLAVE), null).statusCode()).isEqualTo(201);
        HttpResponse<String> login = enviarJson("POST", "/api/auth/login",
                Map.of("correo", cliente.getCorreo(), "password", CLAVE), null);
        assertThat(login.statusCode()).isEqualTo(200);
        return objectMapper.readTree(login.body()).path("token").asText();
    }

    private String iniciarSesionAdministrador() throws Exception {
        String correo = "admin-" + System.nanoTime() + "@example.com";
        usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Administrador de prueba")
                .correo(correo)
                .passwordHash(passwordEncoder.encode(CLAVE))
                .roles(new HashSet<>(Set.of(Rol.ADMINISTRADOR)))
                .build());
        HttpResponse<String> login = enviarJson("POST", "/api/auth/login",
                Map.of("correo", correo, "password", CLAVE), null);
        assertThat(login.statusCode()).isEqualTo(200);
        return objectMapper.readTree(login.body()).path("token").asText();
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