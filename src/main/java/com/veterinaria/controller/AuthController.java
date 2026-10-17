package com.veterinaria.controller;

import com.veterinaria.dto.AuthResponseDTO;
import com.veterinaria.dto.ClienteResponseDTO;
import com.veterinaria.dto.LoginAuthDTO;
import com.veterinaria.dto.RegistroAuthDTO;
import com.veterinaria.service.AuthService;
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
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginAuthDTO solicitud) {
        return ResponseEntity.ok(authService.iniciarSesion(solicitud));
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
