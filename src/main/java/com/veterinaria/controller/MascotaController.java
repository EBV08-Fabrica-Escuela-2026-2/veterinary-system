package com.veterinaria.controller;

import com.veterinaria.dto.MascotaRegistroDTO;
import com.veterinaria.model.Cliente;
import com.veterinaria.model.Mascota;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.service.MascotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mascotas")
@RequiredArgsConstructor
@Tag(name = "Mascotas", description = "API para registrar y consultar mascotas por documento de identidad del cliente")
public class MascotaController {

    private final MascotaService mascotaService;
    private final ClienteRepository clienteRepository;

    @PostMapping
    @Operation(
        summary = "Registrar una mascota",
        description = "Registra una mascota usando el documento de identidad del cliente",
        responses = {
            @ApiResponse(responseCode = "201", description = "Mascota registrada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Campos obligatorios vacíos, edad inválida o observaciones fuera de longitud"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado por documento")
        }
    )
    public ResponseEntity<Map<String, Object>> registrarMascota(
            @Valid @RequestBody MascotaRegistroDTO dto,
            @Parameter(description = "Documento de identidad del cliente")
            @RequestParam("documentoIdentidad") String documentoIdentidad) {
        Cliente cliente = clienteRepository.findByDocumentoIdentidad(documentoIdentidad)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con documento: " + documentoIdentidad));

        Mascota mascota = mascotaService.registrarMascota(dto, cliente.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Mascota registrada exitosamente",
                "idMascota", mascota.getId(),
                "documentoIdentidad", cliente.getDocumentoIdentidad()
        ));
    }

    @GetMapping
    @Operation(
        summary = "Consultar mascotas por documento del cliente",
        description = "Retorna las mascotas asociadas al cliente identificado por su documento de identidad",
        responses = {
            @ApiResponse(responseCode = "200", description = "Listado obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado por documento")
        }
    )
    public ResponseEntity<List<Mascota>> listarMascotas(
            @Parameter(description = "Documento de identidad del cliente")
            @RequestParam("documentoIdentidad") String documentoIdentidad) {
        Cliente cliente = clienteRepository.findByDocumentoIdentidad(documentoIdentidad)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con documento: " + documentoIdentidad));

        return ResponseEntity.ok(mascotaService.listarMascotasPorCliente(cliente.getId()));
    }
}
