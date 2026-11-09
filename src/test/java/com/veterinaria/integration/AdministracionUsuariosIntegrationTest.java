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

/** HU19 - Gestionar usuarios: listado, cambio de estado/roles, auditoría e integridad administrativa. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdministracionUsuariosIntegrationTest {

    private static final String CLAVE = "ClaveSegura123";
    private static final String RUTA = "/api/admin/usuarios";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private VeterinarioRepository veterinarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ControlIntentosLoginService controlIntentos;

    @LocalServerPort private int port;

    private String sufijo;
    private Usuario admin;
    private String tokenAdmin;

    @BeforeEach
    void preparar() throws Exception {
        controlIntentos.reiniciar();
        sufijo = String.valueOf(System.nanoTime());
        admin = crearUsuario("admin-" + sufijo + "@vetagenda.com", null, Rol.ADMINISTRADOR);
        tokenAdmin = iniciarSesion(admin.getCorreo());
    }

    @AfterEach
    void limpiar() {
        controlIntentos.reiniciar();
    }

    @Test
    void soloUnAdministradorPuedeGestionarUsuarios() throws Exception {
        Usuario veterinario = crearVeterinario();
        String tokenVeterinario = iniciarSesion(veterinario.getCorreo());

        assertThat(enviar("GET", RUTA, null, null).statusCode()).isEqualTo(401);
        assertThat(enviar("GET", RUTA, null, tokenVeterinario).statusCode()).isEqualTo(403);
        assertThat(enviar("PATCH", RUTA + "/" + admin.getId() + "/estado", Map.of("activo", false), tokenVeterinario)
                .statusCode()).isEqualTo(403);

        HttpResponse<String> listado = enviar("GET", RUTA + "?buscar=" + sufijo, null, tokenAdmin);
        assertThat(listado.statusCode()).isEqualTo(200);
        JsonNode usuarios = objectMapper.readTree(listado.body());
        assertThat(usuarios).hasSize(2);
        assertThat(listado.body()).contains(admin.getCorreo(), veterinario.getCorreo());
        assertThat(enviar("GET", RUTA + "?rol=SUPERUSUARIO", null, tokenAdmin).statusCode()).isEqualTo(400);
    }

    @Test
    void desactivarUnVeterinarioCierraSuSesionYQuedaAuditado() throws Exception {
        Usuario veterinario = crearVeterinario();
        String tokenVeterinario = iniciarSesion(veterinario.getCorreo());
        assertThat(enviar("GET", "/api/veterinario-portal/perfil", null, tokenVeterinario).statusCode()).isEqualTo(200);

        HttpResponse<String> desactivacion = enviar("PATCH", RUTA + "/" + veterinario.getId() + "/estado",
                Map.of("activo", false), tokenAdmin);
        assertThat(desactivacion.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(desactivacion.body()).path("activo").asBoolean()).isFalse();

        // La sesión abierta queda desconectada de inmediato.
        assertThat(enviar("GET", "/api/veterinario-portal/perfil", null, tokenVeterinario).statusCode()).isEqualTo(401);
        assertThat(login(veterinario.getCorreo()).statusCode()).isEqualTo(401);

        // Reactivar no revive el token anterior: hay que iniciar sesión de nuevo.
        assertThat(enviar("PATCH", RUTA + "/" + veterinario.getId() + "/estado", Map.of("activo", true), tokenAdmin)
                .statusCode()).isEqualTo(200);
        assertThat(enviar("GET", "/api/veterinario-portal/perfil", null, tokenVeterinario).statusCode()).isEqualTo(401);
        String tokenNuevo = iniciarSesion(veterinario.getCorreo());
        assertThat(enviar("GET", "/api/veterinario-portal/perfil", null, tokenNuevo).statusCode()).isEqualTo(200);

        JsonNode auditoria = objectMapper.readTree(
                enviar("GET", RUTA + "/" + veterinario.getId() + "/auditoria", null, tokenAdmin).body());
        assertThat(auditoria).hasSize(2);
        assertThat(auditoria.get(1).path("accion").asText()).isEqualTo("CAMBIO_ESTADO");
        assertThat(auditoria.get(1).path("valorAnterior").asText()).isEqualTo("ACTIVO");
        assertThat(auditoria.get(1).path("valorNuevo").asText()).isEqualTo("INACTIVO");
        assertThat(auditoria.get(1).path("administradorCorreo").asText()).isEqualTo(admin.getCorreo());
        assertThat(auditoria.get(1).path("fecha").asText()).isNotBlank();
    }

    @Test
    void asignarRolAdicionalDeAdministradorAplicaDeInmediatoYQuedaAuditado() throws Exception {
        Usuario veterinario = crearVeterinario();
        String tokenVeterinario = iniciarSesion(veterinario.getCorreo());

        HttpResponse<String> cambio = enviar("PATCH", RUTA + "/" + veterinario.getId() + "/roles",
                Map.of("roles", List.of("VETERINARIO", "ADMINISTRADOR")), tokenAdmin);
        assertThat(cambio.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(cambio.body()).path("roles").toString())
                .isEqualTo("[\"ADMINISTRADOR\",\"VETERINARIO\"]");

        // El mismo token ya tiene permisos de administrador (los roles se leen de la base en cada petición).
        assertThat(enviar("GET", RUTA, null, tokenVeterinario).statusCode()).isEqualTo(200);

        JsonNode auditoria = objectMapper.readTree(
                enviar("GET", RUTA + "/" + veterinario.getId() + "/auditoria", null, tokenAdmin).body());
        assertThat(auditoria.get(0).path("accion").asText()).isEqualTo("CAMBIO_ROLES");
        assertThat(auditoria.get(0).path("valorNuevo").asText()).isEqualTo("ADMINISTRADOR,VETERINARIO");

        assertThat(enviar("PATCH", RUTA + "/" + veterinario.getId() + "/roles",
                Map.of("roles", List.of()), tokenAdmin).statusCode()).isEqualTo(400);
        assertThat(enviar("PATCH", RUTA + "/" + admin.getId() + "/roles",
                Map.of("roles", List.of("VETERINARIO")), tokenAdmin).statusCode()).isEqualTo(409);
    }

    @Test
    void unAdministradorNoPuedeDesactivarseASiMismo() throws Exception {
        HttpResponse<String> respuesta = enviar("PATCH", RUTA + "/" + admin.getId() + "/estado",
                Map.of("activo", false), tokenAdmin);

        assertThat(respuesta.statusCode()).isEqualTo(409);
        assertThat(objectMapper.readTree(respuesta.body()).path("mensaje").asText()).isNotBlank();
        assertThat(usuarioRepository.findById(admin.getId()).orElseThrow().estaActivo()).isTrue();
        assertThat(enviar("GET", RUTA, null, tokenAdmin).statusCode()).isEqualTo(200);
    }

    @Test
    void usuarioInexistenteYCuerpoInvalidoResponden404Y400() throws Exception {
        assertThat(enviar("PATCH", RUTA + "/999999999/estado", Map.of("activo", false), tokenAdmin).statusCode())
                .isEqualTo(404);
        assertThat(enviar("PATCH", RUTA + "/" + admin.getId() + "/estado", Map.of(), tokenAdmin).statusCode())
                .isEqualTo(400);
        assertThat(enviar("GET", RUTA + "/999999999/auditoria", null, tokenAdmin).statusCode()).isEqualTo(404);
    }

    private Usuario crearVeterinario() {
        Veterinario veterinario = veterinarioRepository.saveAndFlush(Veterinario.builder()
                .nombre("Dra. Gestión " + sufijo)
                .documentoIdentidad(sufijo.substring(sufijo.length() - 10))
                .correo("vet-" + sufijo + "@vetagenda.com")
                .activo(true)
                .build());
        return crearUsuario(veterinario.getCorreo(), veterinario, Rol.VETERINARIO);
    }

    private Usuario crearUsuario(String correo, Veterinario veterinario, Rol rol) {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Usuario " + sufijo)
                .correo(correo)
                .passwordHash(passwordEncoder.encode(CLAVE))
                .veterinario(veterinario)
                .activo(true)
                .roles(new HashSet<>(List.of(rol)))
                .build());
    }

    private String iniciarSesion(String correo) throws Exception {
        HttpResponse<String> respuesta = login(correo);
        assertThat(respuesta.statusCode()).isEqualTo(200);
        return objectMapper.readTree(respuesta.body()).path("token").asText();
    }

    private HttpResponse<String> login(String correo) throws Exception {
        return enviar("POST", "/api/auth/login", Map.of("correo", correo, "password", CLAVE), null);
    }

    private HttpResponse<String> enviar(String metodo, String path, Object cuerpo, String token) throws Exception {
        HttpRequest.BodyPublisher publisher = cuerpo == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(cuerpo));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(metodo, publisher);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
