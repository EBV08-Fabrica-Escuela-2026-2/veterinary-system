package com.veterinaria.controller;

import com.veterinaria.dto.AuthResponseDTO;
import com.veterinaria.dto.ClienteResponseDTO;
import com.veterinaria.dto.LoginAuthDTO;
import com.veterinaria.dto.RegistroAuthDTO;
import com.veterinaria.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro de cuentas e inicio de sesión con JWT")
public class AuthController {

    /** Nombre de la cookie de sesión que se limpia al cerrar sesión (si el front la usa). */
    static final String COOKIE_SESION = "access_token";

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
            description = "Valida correo y contraseña (bcrypt) de un Cliente, Veterinario o Administrador y emite un JWT "
                    + "con el rol y la expiración. Tras 5 intentos fallidos consecutivos por cuenta o IP se bloquean "
                    + "los intentos durante 15 minutos.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Credenciales válidas; incluye token, rol y roles"),
                    @ApiResponse(responseCode = "400", description = "Correo o contraseña vacíos o con formato inválido"),
                    @ApiResponse(responseCode = "401", description = "Credenciales incorrectas (mensaje genérico)"),
                    @ApiResponse(responseCode = "429", description = "Demasiados intentos fallidos; ver encabezado Retry-After")
            })
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginAuthDTO solicitud,
            HttpServletRequest request) {
        return ResponseEntity.ok(authService.iniciarSesion(solicitud, request.getRemoteAddr()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegistroAuthDTO solicitud) {
        return ResponseEntity.status(201).body(authService.registrar(solicitud));
    }

    @GetMapping("/me")
    public ResponseEntity<ClienteResponseDTO> me(Authentication authentication) {
        return ResponseEntity.ok(authService.obtenerPerfil(authentication.getName()));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cerrar sesión",
            description = "Invalida el token actual: a partir de este momento el JWT enviado en el encabezado "
                    + "Authorization deja de ser aceptado (también cualquier otro token anterior de la misma cuenta). "
                    + "Además limpia la cookie HttpOnly de sesión. Para volver a entrar hay que iniciar sesión de nuevo.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sesión cerrada; el token ya no sirve"),
                    @ApiResponse(responseCode = "401", description = "No hay sesión activa o el token ya no es válido")
            })
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        authService.cerrarSesion(authentication.getName());

        ResponseCookie cookieLimpia = ResponseCookie.from(COOKIE_SESION, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieLimpia.toString())
                .body(Map.of("mensaje", "Sesión cerrada correctamente"));
    }
}
