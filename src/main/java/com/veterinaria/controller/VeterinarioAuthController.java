package com.veterinaria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.dto.AuthResponseDTO;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.util.JwtTokenService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/veterinario")
@RequiredArgsConstructor
public class VeterinarioAuthController {

    private static final String CORREO_DEMO = "camilo.duran@vetagenda.com";

    private final VeterinarioRepository veterinarioRepository;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/demo")
    public ResponseEntity<AuthResponseDTO> iniciarDemo() {
        Veterinario veterinario = veterinarioRepository.findByCorreoIgnoreCase(CORREO_DEMO)
                .filter(item -> Boolean.TRUE.equals(item.getActivo()))
                .orElseThrow(() -> new ResourceNotFoundException("El perfil veterinario demo no está disponible"));
        String token = jwtTokenService.generarToken(veterinario.getCorreo());
        return ResponseEntity.ok(new AuthResponseDTO("Perfil demo iniciado", token, "Bearer",
                jwtTokenService.getExpiracionMs()));
    }
}
