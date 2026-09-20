package com.veterinaria.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.dto.VeterinarioListaDTO;
import com.veterinaria.dto.VeterinarioRegistroDTO;
import com.veterinaria.service.VeterinarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/veterinarios")
@RequiredArgsConstructor
@Tag(name = "Veterinarios", description = "API para registrar y gestionar veterinarios")
public class VeterinarioController {
    private final VeterinarioService veterinarioService;

    @PostMapping
    @Operation(
        summary = "Registrar un nuevo veterinario",
        description = "Registra un veterinario con estado activo, validando datos obligatorios y duplicados",
        responses = {
            @ApiResponse(responseCode = "201", description = "Registro de veterinario exitoso"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o veterinario/correo ya registrado")
        }
    )
    public ResponseEntity<Map<String, Object>> registrarVeterinario(@Valid @RequestBody VeterinarioRegistroDTO dto) {
        var veterinario = veterinarioService.registrarVeterinario(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Registro de veterinario exitoso",
                "idVeterinario", veterinario.getId()
        ));
    }
    @GetMapping
    @Operation(
        summary = "Listar veterinarios activos",
        description = "Retorna id y nombre de los veterinarios activos, para seleccionarlos al registrar un servicio",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
        }
    )
    public ResponseEntity<List<VeterinarioListaDTO>> listarActivos() {
        return ResponseEntity.ok(veterinarioService.listarActivos());
    }
}
