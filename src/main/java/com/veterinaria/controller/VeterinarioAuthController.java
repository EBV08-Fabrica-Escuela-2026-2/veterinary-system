package com.veterinaria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.dto.AuthResponseDTO;
import com.veterinaria.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/veterinario")
@RequiredArgsConstructor
public class VeterinarioAuthController {

    private static final String CORREO_DEMO = "camilo.duran@vetagenda.com";

    private final AuthService authService;

    @PostMapping("/demo")
    public ResponseEntity<AuthResponseDTO> iniciarDemo() {
        return ResponseEntity.ok(authService.iniciarSesionVeterinarioDemo(CORREO_DEMO));
    }
}
