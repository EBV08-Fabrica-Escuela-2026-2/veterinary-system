package com.veterinaria.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veterinaria.model.Cliente;
import com.veterinaria.repository.ClienteRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

        private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

        @LocalServerPort
        private int port;

    @Test
    void tokenDeLoginAutorizaLasRutasDeReprogramacion() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String correo = "auth" + suffix + "@example.com";
        String documento = suffix.substring(suffix.length() - 8);
        clienteRepository.saveAndFlush(Cliente.builder()
                .nombre("Cliente de autenticación")
                .documentoIdentidad(documento)
                .telefono("3001234567")
                .correo(correo)
                .activo(true)
                .build());

        HttpResponse<String> registro = enviarJson("/api/auth/register", Map.of(
                "correo", correo,
                "documentoIdentidad", documento,
                "password", "ClaveSegura123"), null);
        assertThat(registro.statusCode()).isEqualTo(201);

        HttpResponse<String> login = enviarJson("/api/auth/login", Map.of(
                "correo", correo,
                "password", "ClaveSegura123"), null);
        assertThat(login.statusCode()).isEqualTo(200);
        JsonNode response = objectMapper.readTree(login.body());
        String token = response.path("token").asText();

        HttpResponse<String> perfilSinToken = enviarGet("/api/auth/me", null);
        assertThat(perfilSinToken.statusCode()).isEqualTo(401);

        HttpResponse<String> perfilConToken = enviarGet("/api/auth/me", token);
        assertThat(perfilConToken.statusCode()).isEqualTo(200);
        JsonNode perfil = objectMapper.readTree(perfilConToken.body());
        assertThat(perfil.path("correo").asText()).isEqualTo(correo);
        assertThat(perfil.path("documentoIdentidad").asText()).isEqualTo(documento);

        HttpResponse<String> sinToken = enviarGet("/api/citas/999/disponibilidad?fecha=2026-10-20", null);
        assertThat(sinToken.statusCode()).isEqualTo(401);

        HttpResponse<String> conToken = enviarGet("/api/citas/999/disponibilidad?fecha=2026-10-20", token);
        assertThat(conToken.statusCode()).isEqualTo(404);
    }

    @Test
    void credencialSembradaDeCarolinaUsaUnHashBcryptValido() {
        assertThat(passwordEncoder.matches(
                "Demo2026!",
                "$2y$10$RnNxY7Vyu9xDwJk0wjXKYeht3z7GDYVCsM0HEOWQh.E96j/ryyxiS"))
                .isTrue();
    }

    private HttpResponse<String> enviarJson(String path, Object body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> enviarGet(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
