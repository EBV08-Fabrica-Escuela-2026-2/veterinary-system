package com.veterinaria.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DeploymentFlowIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void flujoClienteMascotaVeterinarioServicioCatalogoFunciona() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String documentoCliente = "10" + suffix.substring(suffix.length() - 8);

        HttpResponse<String> clienteResponse = postJson("/api/clientes", Map.of(
                "nombre", "Ana Sprint",
                "documentoIdentidad", documentoCliente,
                "telefono", "3001234567",
                "correo", "ana" + suffix + "@example.com",
                "direccion", "Calle 123"
        ));
        assertThat(clienteResponse.statusCode()).isEqualTo(201);

        HttpResponse<String> mascotaResponse = postJson(
                "/api/mascotas?documentoIdentidad=" + documentoCliente,
                Map.of(
                        "nombre", "Milo",
                        "especie", "Gato",
                        "sexo", "Macho",
                        "raza", "Criollo",
                        "edad", 3,
                        "observaciones", "Paciente tranquilo"
                ));
        assertThat(mascotaResponse.statusCode()).isEqualTo(201);

        HttpResponse<String> veterinarioResponse = postJson("/api/veterinarios", Map.of(
                "nombre", "Dra. Sprint Review",
                "tipoDocumento", "CC",
                "documentoIdentidad", "20" + suffix.substring(suffix.length() - 8),
                "telefono", "3109876543",
                "correo", "vet" + suffix + "@example.com",
                "tarjetaProfesional", "TP-" + suffix.substring(suffix.length() - 6),
                "especialidad", "Medicina general",
                "direccion", "Avenida 45",
                "horarioAtencion", "Lunes a viernes"
        ));
        assertThat(veterinarioResponse.statusCode()).isEqualTo(201);

        long veterinarioId = objectMapper
                .readTree(veterinarioResponse.body())
                .path("idVeterinario")
                .asLong();

        HttpResponse<String> servicioResponse = postJson("/api/servicios", Map.of(
                "veterinarioId", veterinarioId,
                "nombre", "Consulta Sprint",
                "descripcion", "Servicio validado para Sprint Review",
                "precio", 65000,
                "duracionMinutos", 30
        ));
        assertThat(servicioResponse.statusCode()).isEqualTo(201);

        HttpResponse<String> catalogoResponse = send(HttpRequest.newBuilder(uri("/api/catalogo")).GET().build());
        assertThat(catalogoResponse.statusCode()).isEqualTo(200);

        JsonNode catalogo = objectMapper.readTree(catalogoResponse.body());
        assertThat(catalogo.path("moneda").asText()).isEqualTo("COP");
        assertThat(catalogoContieneServicio(catalogo, veterinarioId, "Consulta Sprint")).isTrue();
    }

    @Test
    void corsPermiteOrigenLocalDelFrontend() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/catalogo"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .build();

        HttpResponse<String> response = send(request);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
                .contains("http://localhost:5173");
    }

    private boolean catalogoContieneServicio(JsonNode catalogo, long veterinarioId, String servicioNombre) {
        return StreamSupport.stream(catalogo.path("veterinarios").spliterator(), false)
                .filter(veterinario -> veterinario.path("id").asLong() == veterinarioId)
                .flatMap(veterinario -> StreamSupport.stream(veterinario.path("servicios").spliterator(), false))
                .anyMatch(servicio -> servicioNombre.equals(servicio.path("nombre").asText()));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private HttpResponse<String> postJson(String path, Object body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(json(body)))
                .build();
        return send(request);
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
