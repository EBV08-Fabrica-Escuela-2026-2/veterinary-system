package com.veterinaria.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veterinaria.model.Cita;
import com.veterinaria.model.Cliente;
import com.veterinaria.model.HorarioAtencion;
import com.veterinaria.model.Mascota;
import com.veterinaria.model.Rol;
import com.veterinaria.model.Servicio;
import com.veterinaria.model.Usuario;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.CitaRepository;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.repository.HorarioAtencionRepository;
import com.veterinaria.repository.MascotaRepository;
import com.veterinaria.repository.ServicioRepository;
import com.veterinaria.repository.UsuarioRepository;
import com.veterinaria.repository.VeterinarioRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OcupacionControllerIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VeterinarioRepository veterinarioRepository;

    @Autowired
    private HorarioAtencionRepository horarioAtencionRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @LocalServerPort
    private int port;

    @Test
    void administradorPuedeConsultarResumenDeOcupacion() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String documentoSuffix = suffix.substring(suffix.length() - 8);
        String correo = "admin-ocupacion-" + suffix + "@example.com";
        crearUsuario(correo, Rol.ADMINISTRADOR);

        Veterinario veterinario = veterinarioRepository.save(Veterinario.builder()
                .nombre("Dr. Prueba")
                .documentoIdentidad("V" + documentoSuffix)
                .correo("vet-ocupacion-" + suffix + "@example.com")
                .telefono("3000000000")
                .tipoDocumento("CC")
                .activo(true)
                .build());

        horarioAtencionRepository.save(HorarioAtencion.builder()
                .veterinario(veterinario)
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(10, 0))
                .activo(true)
                .build());

        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente ocupación")
                .documentoIdentidad("C" + documentoSuffix)
                .telefono("3000000001")
                .correo("cliente-ocupacion-" + suffix + "@example.com")
                .activo(true)
                .build());

        Mascota mascota = mascotaRepository.save(Mascota.builder()
                .nombre("Luna")
                .especie("Perro")
                .raza("Mestizo")
                .edad(4)
                .sexo("Hembra")
                .cliente(cliente)
                .build());

        Servicio servicio = servicioRepository.save(Servicio.builder()
                .veterinario(veterinario)
                .nombre("Consulta General")
                .descripcion("Consulta de prueba")
                .precio(BigDecimal.valueOf(85000))
                .duracionMinutos(30)
                .activo(true)
                .build());

        citaRepository.save(Cita.builder()
                .cliente(cliente)
                .mascota(mascota)
                .veterinario(veterinario)
                .servicio(servicio)
                .fechaHoraInicio(LocalDateTime.of(2026, 11, 2, 9, 0))
                .fechaHoraFin(LocalDateTime.of(2026, 11, 2, 9, 30))
                .estado("CONFIRMADA")
                .build());

        String token = login(correo);
        HttpResponse<String> response = enviarGet(
                "/api/ocupacion/resumen?fechaInicio=2026-11-02&fechaFin=2026-11-02&agrupacion=DIA&veterinarioId="
                        + veterinario.getId(),
                token);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode resultado = objectMapper.readTree(response.body());
        assertThat(resultado.path("totalSlotsHabilitados").asLong()).isEqualTo(4);
        assertThat(resultado.path("totalCitasEfectivas").asLong()).isEqualTo(1);
        assertThat(resultado.path("totalSlotsDisponibles").asLong()).isEqualTo(3);
        assertThat(resultado.path("porcentajeOcupacion").asDouble()).isEqualTo(25.0);
    }

    @Test
    void clientesNoPuedenConsultarOcupacion() throws Exception {
        String correo = "cliente-ocupacion-negado-" + System.nanoTime() + "@example.com";
        crearUsuario(correo, Rol.CLIENTE);

        HttpResponse<String> response = enviarGet(
                "/api/ocupacion/resumen?fechaInicio=2026-11-02&fechaFin=2026-11-02", login(correo));

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void consultaSinRegistrosDevuelveMensajeInformativo() throws Exception {
        String correo = "admin-sin-registros-" + System.nanoTime() + "@example.com";
        crearUsuario(correo, Rol.ADMINISTRADOR);
        String suffix = String.valueOf(System.nanoTime());
        Veterinario veterinario = veterinarioRepository.save(Veterinario.builder()
                .nombre("Dr. Sin agenda")
                .documentoIdentidad("S" + suffix.substring(suffix.length() - 8))
                .correo("vet-sin-agenda-" + suffix + "@example.com")
                .tipoDocumento("CC")
                .activo(true)
                .build());

        HttpResponse<String> response = enviarGet(
                "/api/ocupacion/resumen?fechaInicio=2026-11-20&fechaFin=2026-11-25&veterinarioId="
                        + veterinario.getId(),
                login(correo));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode resultado = objectMapper.readTree(response.body());
        assertThat(resultado.path("mensaje").asText())
                .isEqualTo("No hay registros de turnos para el periodo seleccionado");
        assertThat(resultado.path("totalSlotsHabilitados").asLong()).isZero();
        assertThat(resultado.path("distribucion").isEmpty()).isTrue();
    }

    @Test
    void distribucionAgrupaPorDiaSemanaYMesYExcluyeCitasAnuladas() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String correo = "admin-distribucion-" + suffix + "@example.com";
        crearUsuario(correo, Rol.ADMINISTRADOR);
        Veterinario veterinario = crearVeterinarioConAgendaYCitas(suffix);
        String token = login(correo);
        String filtro = "&veterinarioId=" + veterinario.getId();

        HttpResponse<String> diaria = enviarGet(
                "/api/ocupacion/distribucion?fechaInicio=2026-11-02&fechaFin=2026-11-09&agrupacion=DIA"
                        + filtro,
                token);
        assertThat(diaria.statusCode()).isEqualTo(200);
        JsonNode resultadoDiario = objectMapper.readTree(diaria.body());
        assertThat(resultadoDiario.path("totalSlotsHabilitados").asLong()).isEqualTo(8);
        assertThat(resultadoDiario.path("totalCitasEfectivas").asLong()).isEqualTo(2);
        assertThat(resultadoDiario.path("distribucion").size()).isEqualTo(8);
        assertThat(resultadoDiario.path("distribucion").get(0).path("periodo").asText())
                .isEqualTo("2026-11-02");
        assertThat(resultadoDiario.path("distribucion").get(0).path("citasEfectivas").asLong()).isEqualTo(1);

        HttpResponse<String> semanal = enviarGet(
                "/api/ocupacion/distribucion?fechaInicio=2026-11-02&fechaFin=2026-11-09&agrupacion=SEMANA"
                        + filtro,
                token);
        assertThat(semanal.statusCode()).isEqualTo(200);
        JsonNode resultadoSemanal = objectMapper.readTree(semanal.body());
        assertThat(resultadoSemanal.path("distribucion").size()).isEqualTo(2);
        assertThat(resultadoSemanal.path("distribucion").get(0).path("periodo").asText()).isEqualTo("2026-W45");
        assertThat(resultadoSemanal.path("distribucion").get(1).path("periodo").asText()).isEqualTo("2026-W46");
        assertThat(resultadoSemanal.path("distribucion").get(0).path("fechaInicio").asText())
                .isEqualTo("2026-11-02");
        assertThat(resultadoSemanal.path("distribucion").get(0).path("fechaFin").asText())
                .isEqualTo("2026-11-08");

        HttpResponse<String> mensual = enviarGet(
                "/api/ocupacion/distribucion?fechaInicio=2026-11-02&fechaFin=2026-12-07&agrupacion=MES"
                        + filtro,
                token);
        assertThat(mensual.statusCode()).isEqualTo(200);
        JsonNode resultadoMensual = objectMapper.readTree(mensual.body());
        assertThat(resultadoMensual.path("distribucion").size()).isEqualTo(2);
        assertThat(resultadoMensual.path("distribucion").get(0).path("periodo").asText()).isEqualTo("2026-11");
        assertThat(resultadoMensual.path("distribucion").get(0).path("slotsHabilitados").asLong()).isEqualTo(20);
        assertThat(resultadoMensual.path("distribucion").get(1).path("periodo").asText()).isEqualTo("2026-12");
        assertThat(resultadoMensual.path("distribucion").get(1).path("slotsHabilitados").asLong()).isEqualTo(4);
    }

    @Test
    void rechazaFiltrosQueElModeloNoPuedeAplicar() throws Exception {
        String correo = "admin-filtros-" + System.nanoTime() + "@example.com";
        crearUsuario(correo, Rol.ADMINISTRADOR);
        String token = login(correo);

        HttpResponse<String> sede = enviarGet(
                "/api/ocupacion/resumen?fechaInicio=2026-11-02&fechaFin=2026-11-02&sedeId=1", token);
        HttpResponse<String> agrupacion = enviarGet(
                "/api/ocupacion/resumen?fechaInicio=2026-11-02&fechaFin=2026-11-02&agrupacion=ANUAL", token);

        assertThat(sede.statusCode()).isEqualTo(400);
        assertThat(agrupacion.statusCode()).isEqualTo(400);
    }

    @Test
    void veterinariosNoPuedenConsultarOcupacion() throws Exception {
        String correo = "veterinario-ocupacion-negado-" + System.nanoTime() + "@example.com";
        crearUsuario(correo, Rol.VETERINARIO);

        HttpResponse<String> response = enviarGet(
                "/api/ocupacion/distribucion?fechaInicio=2026-11-02&fechaFin=2026-11-02", login(correo));

        assertThat(response.statusCode()).isEqualTo(403);
    }

    private Veterinario crearVeterinarioConAgendaYCitas(String suffix) {
        String documentoSuffix = suffix.substring(suffix.length() - 8);
        Veterinario veterinario = veterinarioRepository.save(Veterinario.builder()
                .nombre("Dr. Distribución")
                .documentoIdentidad("D" + documentoSuffix)
                .correo("vet-distribucion-" + suffix + "@example.com")
                .telefono("3000000002")
                .tipoDocumento("CC")
                .activo(true)
                .build());

        horarioAtencionRepository.save(HorarioAtencion.builder()
                .veterinario(veterinario)
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(10, 0))
                .activo(true)
                .build());

        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente distribución")
                .documentoIdentidad("E" + documentoSuffix)
                .telefono("3000000003")
                .correo("cliente-distribucion-" + suffix + "@example.com")
                .activo(true)
                .build());
        Mascota mascota = mascotaRepository.save(Mascota.builder()
                .nombre("Max")
                .especie("Perro")
                .raza("Mestizo")
                .edad(3)
                .sexo("Macho")
                .cliente(cliente)
                .build());
        Servicio servicio = servicioRepository.save(Servicio.builder()
                .veterinario(veterinario)
                .nombre("Consulta distribución")
                .descripcion("Prueba de agrupación")
                .precio(BigDecimal.valueOf(50000))
                .duracionMinutos(30)
                .activo(true)
                .build());

        guardarCita(veterinario, cliente, mascota, servicio, LocalDateTime.of(2026, 11, 2, 9, 0), "CONFIRMADA");
        guardarCita(veterinario, cliente, mascota, servicio, LocalDateTime.of(2026, 11, 9, 9, 0), "COMPLETADA");
        guardarCita(veterinario, cliente, mascota, servicio, LocalDateTime.of(2026, 11, 9, 9, 30), "ANULADA");
        return veterinario;
    }

    private void guardarCita(Veterinario veterinario, Cliente cliente, Mascota mascota, Servicio servicio,
            LocalDateTime inicio, String estado) {
        citaRepository.save(Cita.builder()
                .cliente(cliente)
                .mascota(mascota)
                .veterinario(veterinario)
                .servicio(servicio)
                .fechaHoraInicio(inicio)
                .fechaHoraFin(inicio.plusMinutes(30))
                .estado(estado)
                .build());
    }

    private void crearUsuario(String correo, Rol rol) {
        Usuario usuario = Usuario.builder()
                .nombre("Usuario prueba")
                .correo(correo)
                .passwordHash(passwordEncoder.encode("ClaveSegura123"))
                .activo(true)
                .build();
        usuario.getRoles().add(rol);
        usuarioRepository.save(usuario);
    }

    private String login(String correo) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of(
                        "correo", correo,
                        "password", "ClaveSegura123"))))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body()).path("token").asText();
    }

    private HttpResponse<String> enviarGet(String path, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
