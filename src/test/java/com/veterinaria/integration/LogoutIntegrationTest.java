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

/** HU04 - Cerrar sesión: el token deja de servir y la cookie de sesión se limpia. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LogoutIntegrationTest {

    private static final String CLAVE = "ClaveSegura123";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private VeterinarioRepository veterinarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ControlIntentosLoginService controlIntentos;

    @LocalServerPort private int port;

    @BeforeEach
    @AfterEach
    void limpiarIntentos() {
        controlIntentos.reiniciar();
    }

    @Test
    void trasCerrarSesionElTokenDejaDeServirYSeLimpiaLaCookie() throws Exception {
        Usuario usuario = crearVeterinarioConCuenta();
        String token = tokenDe(login(usuario.getCorreo()));
        assertThat(enviarGet("/api/veterinario-portal/perfil", token).statusCode()).isEqualTo(200);

        HttpResponse<String> logout = enviarLogout(token);

        assertThat(logout.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(logout.body()).path("mensaje").asText())
                .isEqualTo("Sesión cerrada correctamente");
        assertThat(logout.headers().firstValue("Set-Cookie")).hasValueSatisfying(cookie -> {
            assertThat(cookie).contains("access_token=").contains("Max-Age=0").contains("HttpOnly");
        });
        assertThat(enviarGet("/api/veterinario-portal/perfil", token).statusCode()).isEqualTo(401);
        assertThat(enviarLogout(token).statusCode()).isEqualTo(401);
    }

    @Test
    void despuesDeCerrarSesionSePuedeIniciarSesionDeNuevo() throws Exception {
        Usuario usuario = crearVeterinarioConCuenta();
        String tokenViejo = tokenDe(login(usuario.getCorreo()));
        assertThat(enviarLogout(tokenViejo).statusCode()).isEqualTo(200);

        String tokenNuevo = tokenDe(login(usuario.getCorreo()));

        assertThat(enviarGet("/api/veterinario-portal/perfil", tokenNuevo).statusCode()).isEqualTo(200);
        assertThat(enviarGet("/api/veterinario-portal/perfil", tokenViejo).statusCode()).isEqualTo(401);
    }

    @Test
    void cerrarSesionSinTokenRespondeNoAutenticado() throws Exception {
        HttpResponse<String> respuesta = enviarLogout(null);

        assertThat(respuesta.statusCode()).isEqualTo(401);
        JsonNode cuerpo = objectMapper.readTree(respuesta.body());
        assertThat(cuerpo.path("mensaje").asText()).isEqualTo("Se requiere iniciar sesión");
    }

    private Usuario crearVeterinarioConCuenta() {
        String sufijo = String.valueOf(System.nanoTime());
        Veterinario veterinario = veterinarioRepository.saveAndFlush(Veterinario.builder()
                .nombre("Dra. Logout")
                .documentoIdentidad(sufijo.substring(sufijo.length() - 10))
                .correo("logout-" + sufijo + "@vetagenda.com")
                .activo(true)
                .build());
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Usuario veterinario")
                .correo(veterinario.getCorreo())
                .passwordHash(passwordEncoder.encode(CLAVE))
                .veterinario(veterinario)
                .activo(true)
                .roles(new HashSet<>(List.of(Rol.VETERINARIO)))
                .build());
    }

    private String tokenDe(HttpResponse<String> respuestaLogin) throws Exception {
        assertThat(respuestaLogin.statusCode()).isEqualTo(200);
        return objectMapper.readTree(respuestaLogin.body()).path("token").asText();
    }

    private HttpResponse<String> login(String correo) throws Exception {
        String cuerpo = objectMapper.writeValueAsString(Map.of("correo", correo, "password", CLAVE));
        return httpClient.send(HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> enviarLogout(String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri("/api/auth/logout"))
                .POST(HttpRequest.BodyPublishers.noBody());
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> enviarGet(String path, String token) throws Exception {
        return httpClient.send(HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
