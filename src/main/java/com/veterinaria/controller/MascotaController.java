package com.veterinaria.controller;

import com.veterinaria.dto.HistorialMascotaDTO;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}/historial")
    @Operation(
        summary = "Consultar el historial de una mascota",
        description = "Retorna el historial cronológico descendente de las citas en estado terminal de la mascota. "
                + "Solo el cliente titular o un veterinario con citas previas con la mascota pueden consultarlo.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de paginación inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene permisos sobre la mascota"),
            @ApiResponse(responseCode = "404", description = "Mascota no encontrada")
        }
    )
    public ResponseEntity<HistorialMascotaDTO> consultarHistorial(
            @Parameter(description = "Identificador de la mascota")
            @PathVariable Long id,
            @Parameter(description = "Número de página (base 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de registros por página")
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        String correoAutenticado = authentication == null ? null : authentication.getName();
        return ResponseEntity.ok(mascotaService.consultarHistorial(id, correoAutenticado, page, limit));
    }
}
