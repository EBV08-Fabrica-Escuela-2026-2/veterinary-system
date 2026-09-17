package com.veterinaria.controller;

import com.veterinaria.dto.ClienteRegistroDTO;
import com.veterinaria.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "API para registrar y gestionar clientes")
public class ClienteController {
    private final ClienteService clienteService;

    @PostMapping
    @Operation(
        summary = "Registrar un nuevo cliente",
        description = "Registra un cliente con estado activo, validando datos obligatorios y duplicados",
        responses = {
            @ApiResponse(responseCode = "201", description = "Registro de cliente exitoso"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o cliente/correo ya registrado")
        }
    )
    public ResponseEntity<String> registrarCliente(@Valid @RequestBody ClienteRegistroDTO dto) {
        clienteService.registrarCliente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Registro de cliente exitoso");
    }
}
