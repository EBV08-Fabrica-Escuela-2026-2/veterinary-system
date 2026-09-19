package com.veterinaria.controller;

import com.veterinaria.dto.ServicioRegistroDTO;
import com.veterinaria.service.ServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
@Tag(name = "Servicios", description = "API para registrar y gestionar servicios veterinarios")
public class ServicioController {

    private final ServicioService servicioService;

    @PostMapping
    @Operation(
        summary = "Registrar un nuevo servicio",
        description = "Registra un servicio veterinario asociado a un veterinario existente, con estado activo",
        responses = {
            @ApiResponse(responseCode = "201", description = "Registro de servicio exitoso"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o veterinario inactivo"),
            @ApiResponse(responseCode = "404", description = "Veterinario no encontrado")
        }
    )
    public ResponseEntity<String> registrarServicio(@Valid @RequestBody ServicioRegistroDTO dto) {
        servicioService.registrarServicio(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Registro de servicio exitoso");
    }
}
