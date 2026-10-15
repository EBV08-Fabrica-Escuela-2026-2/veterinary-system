package com.veterinaria.controller;

import com.veterinaria.dto.CitaRegistroDTO;
import com.veterinaria.model.Cita;
import com.veterinaria.model.Cliente;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.service.CitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
@Tag(name = "Citas", description = "API para reservar citas veterinarias")
public class CitaController {

    private final CitaService citaService;
    private final ClienteRepository clienteRepository;

    @PostMapping
    @Operation(summary = "Reservar una cita", description = "Registra una cita para una mascota del cliente, identificado por su documento de identidad", responses = {
            @ApiResponse(responseCode = "201", description = "Cita reservada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, entidades inactivas o mascota/servicio que no corresponden"),
            @ApiResponse(responseCode = "404", description = "Cliente, mascota, veterinario o servicio no encontrado"),
            @ApiResponse(responseCode = "409", description = "El veterinario ya tiene una cita confirmada en ese horario")
    })
    public ResponseEntity<Map<String, Object>> reservarCita(
            @Valid @RequestBody CitaRegistroDTO dto,
            @Parameter(description = "Documento de identidad del cliente") @RequestParam("documentoIdentidad") String documentoIdentidad) {
        Cliente cliente = clienteRepository.findByDocumentoIdentidad(documentoIdentidad)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cliente no encontrado con documento: " + documentoIdentidad));

        Cita cita = citaService.reservarCita(dto, cliente.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Cita reservada exitosamente",
                "idCita", cita.getId(),
                "fechaHoraInicio", cita.getFechaHoraInicio(),
                "fechaHoraFin", cita.getFechaHoraFin(),
                "estado", cita.getEstado()));
    }
}