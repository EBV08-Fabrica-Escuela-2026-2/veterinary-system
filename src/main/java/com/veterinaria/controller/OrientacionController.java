package com.veterinaria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.dto.OrientacionRequestDTO;
import com.veterinaria.dto.OrientacionResponseDTO;
import com.veterinaria.service.OrientacionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orientacion")
@RequiredArgsConstructor
@Tag(name = "Orientación inteligente", description = "Orientación inicial de atención para mascotas")
public class OrientacionController {

    private final OrientacionService orientacionService;

    @PostMapping
    @Operation(summary = "Analizar síntomas y sugerir servicios activos del catálogo")
    public ResponseEntity<OrientacionResponseDTO> orientar(
            Authentication authentication,
            @Valid @RequestBody OrientacionRequestDTO solicitud) {
        return ResponseEntity.ok(orientacionService.orientar(authentication.getName(), solicitud));
    }
}
