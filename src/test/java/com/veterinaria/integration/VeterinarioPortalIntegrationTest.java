package com.veterinaria.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
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
import com.veterinaria.model.Mascota;
import com.veterinaria.model.Servicio;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.CitaRepository;
import com.veterinaria.repository.ClienteRepository;
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

    @LocalServerPort private int port;

    private Cita cita;

    @BeforeEach
    void prepararDatos() {
        Veterinario veterinario = veterinarioRepository.findByCorreoIgnoreCase("camilo.duran@vetagenda.com")
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
        Servicio servicio = servicioRepository.saveAndFlush(Servicio.builder()
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
