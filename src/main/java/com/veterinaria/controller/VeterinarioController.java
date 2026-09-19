package com.veterinaria.controller;

import com.veterinaria.dto.VeterinarioRegistroDTO;
import com.veterinaria.service.VeterinarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
}
