package com.veterinaria.controller;

import com.veterinaria.dto.AuthResponseDTO;
import com.veterinaria.dto.ClienteResponseDTO;
import com.veterinaria.dto.LoginAuthDTO;
import com.veterinaria.dto.RegistroAuthDTO;
import com.veterinaria.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
