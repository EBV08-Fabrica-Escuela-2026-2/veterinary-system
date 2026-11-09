package com.veterinaria.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veterinaria.model.Rol;
import com.veterinaria.model.Usuario;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.UsuarioRepository;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.service.ControlIntentosLoginService;
import com.veterinaria.util.JwtTokenService;

/** HU03 - Iniciar sesión: login por rol, mensaje genérico y bloqueo por fuerza bruta. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LoginIntegrationTest {

    private static final String CLAVE = "ClaveSegura123";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private VeterinarioRepository veterinarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private ControlIntentosLoginService controlIntentos;

    @LocalServerPort private int port;

    @BeforeEach
    @AfterEach
    void limpiarIntentos() {
        // Todas las pruebas llegan desde 127.0.0.1: se limpia para no bloquear otras clases de prueba.
        controlIntentos.reiniciar();
    }

    @Test
    void administradorYVeterinarioInicianSesionYRecibenSuRol() throws Exception {
        String sufijo = String.valueOf(System.nanoTime());
        Usuario admin = crearUsuario("admin-" + sufijo + "@vetagenda.com", null, Rol.ADMINISTRADOR);
        Veterinario veterinario = veterinarioRepository.saveAndFlush(Veterinario.builder()
                .nombre("Dra. Login")
                .documentoIdentidad(sufijo.substring(sufijo.length() - 10))
                .correo("vet-" + sufijo + "@vetagenda.com")
                .activo(true)
                .build());
        crearUsuario(veterinario.getCorreo(), veterinario, Rol.VETERINARIO);

        HttpResponse<String> loginAdmin = login(admin.getCorreo(), CLAVE);
        assertThat(loginAdmin.statusCode()).isEqualTo(200);
        JsonNode cuerpoAdmin = objectMapper.readTree(loginAdmin.body());
        assertThat(cuerpoAdmin.path("rol").asText()).isEqualTo("ADMINISTRADOR");
        assertThat(cuerpoAdmin.path("tipoToken").asText()).isEqualTo("Bearer");
        assertThat(jwtTokenService.validarYObtenerClaims(cuerpoAdmin.path("token").asText())
                .get(JwtTokenService.CLAIM_ROLES, List.class)).containsExactly("ADMINISTRADOR");

        HttpResponse<String> loginVet = login(veterinario.getCorreo().toUpperCase(), CLAVE);
        assertThat(loginVet.statusCode()).isEqualTo(200);
        JsonNode cuerpoVet = objectMapper.readTree(loginVet.body());
        assertThat(cuerpoVet.path("rol").asText()).isEqualTo("VETERINARIO");

        HttpResponse<String> agenda = enviarGet("/api/veterinario-portal/perfil", cuerpoVet.path("token").asText());
        assertThat(agenda.statusCode()).isEqualTo(200);
        assertThat(agenda.body()).contains(veterinario.getCorreo());
    }

    @Test
    void credencialesInvalidasNoRevelanSiFallaElCorreoOLaContraseña() throws Exception {
        Usuario usuario = crearUsuario("generico-" + System.nanoTime() + "@vetagenda.com", null, Rol.ADMINISTRADOR);

        HttpResponse<String> claveErronea = login(usuario.getCorreo(), "OtraClave999");
        HttpResponse<String> correoInexistente = login("no-existe-" + System.nanoTime() + "@vetagenda.com", CLAVE);

        assertThat(claveErronea.statusCode()).isEqualTo(401);
        assertThat(correoInexistente.statusCode()).isEqualTo(401);
        assertThat(mensaje(claveErronea)).isEqualTo("Credenciales incorrectas");
        assertThat(mensaje(correoInexistente)).isEqualTo(mensaje(claveErronea));
    }

    @Test
    void tokenDeCuentaInactivaYaNoAutoriza() throws Exception {
        Usuario usuario = crearUsuario("inactivo-" + System.nanoTime() + "@vetagenda.com", null, Rol.ADMINISTRADOR);
        String token = objectMapper.readTree(login(usuario.getCorreo(), CLAVE).body()).path("token").asText();

        usuario.setActivo(false);
        usuarioRepository.saveAndFlush(usuario);

        assertThat(login(usuario.getCorreo(), CLAVE).statusCode()).isEqualTo(401);
        assertThat(enviarGet("/api/veterinario-portal/perfil", token).statusCode()).isEqualTo(401);
    }

    @Test
    void sextoIntentoTrasCincoFallosRespondeTooManyRequestsInclusoConLaClaveCorrecta() throws Exception {
        Usuario usuario = crearUsuario("fuerza-" + System.nanoTime() + "@vetagenda.com", null, Rol.ADMINISTRADOR);

        for (int intento = 1; intento <= 5; intento++) {
            assertThat(login(usuario.getCorreo(), "ClaveErronea" + intento).statusCode()).isEqualTo(401);
        }

        HttpResponse<String> sexto = login(usuario.getCorreo(), CLAVE);
        assertThat(sexto.statusCode()).isEqualTo(429);
        assertThat(sexto.headers().firstValue("Retry-After")).hasValueSatisfying(
                valor -> assertThat(Long.parseLong(valor)).isBetween(1L, 900L));
        JsonNode cuerpo = objectMapper.readTree(sexto.body());
        assertThat(cuerpo.path("mensaje").asText()).startsWith("Demasiados intentos fallidos");
        assertThat(cuerpo.path("segundosRestantes").asLong()).isPositive();
        assertThat(cuerpo.has("token")).isFalse();
    }

    private Usuario crearUsuario(String correo, Veterinario veterinario, Rol rol) {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Usuario " + rol.name().toLowerCase())
                .correo(correo)
                .passwordHash(passwordEncoder.encode(CLAVE))
                .veterinario(veterinario)
                .activo(true)
                .roles(new HashSet<>(List.of(rol)))
                .build());
    }

    private String mensaje(HttpResponse<String> respuesta) throws Exception {
        return objectMapper.readTree(respuesta.body()).path("mensaje").asText();
    }

    private HttpResponse<String> login(String correo, String clave) throws Exception {
        String cuerpo = objectMapper.writeValueAsString(Map.of("correo", correo, "password", clave));
        return httpClient.send(HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build(), HttpResponse.BodyHandlers.ofString());
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
