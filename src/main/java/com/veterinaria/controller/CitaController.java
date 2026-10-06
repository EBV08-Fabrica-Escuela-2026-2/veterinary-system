package com.veterinaria.controller;

import com.veterinaria.dto.CancelacionCitaDTO;
import com.veterinaria.dto.CitaClienteDTO;
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

    @GetMapping
    @Operation(summary = "Listar las citas del cliente", description = "Devuelve las citas del cliente identificado por su documento de identidad, ordenadas por fecha de inicio descendente", responses = {
            @ApiResponse(responseCode = "200", description = "Citas listadas exitosamente"),
            @ApiResponse(responseCode = "400", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "401", description = "Falta el parámetro documentoIdentidad")
    })
    public ResponseEntity<List<CitaClienteDTO>> listarCitas(
            @Parameter(description = "Documento de identidad del cliente") @RequestParam("documentoIdentidad") String documentoIdentidad) {
        Cliente cliente = clienteRepository.findByDocumentoIdentidad(documentoIdentidad)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cliente no encontrado con documento: " + documentoIdentidad));

        return ResponseEntity.ok(citaService.listarCitasPorCliente(cliente.getId()));
    }

    @PostMapping("/{idCita}/cancelar")
    @Operation(summary = "Cancelar una cita", description = "Anula una cita del cliente identificado por su documento de identidad. Debe realizarse con al menos 2 horas de anticipación al inicio.", responses = {
            @ApiResponse(responseCode = "200", description = "Cita anulada exitosamente"),
            @ApiResponse(responseCode = "400", description = "La cita no pertenece al cliente especificado o el documento es inválido"),
            @ApiResponse(responseCode = "404", description = "Cliente o cita no encontrada"),
            @ApiResponse(responseCode = "409", description = "La cita ya está anulada o faltan menos de 2 horas para su inicio")
    })
    public ResponseEntity<CancelacionCitaDTO> cancelarCita(
            @Parameter(description = "Id de la cita a anular") @PathVariable Long idCita,
            @Parameter(description = "Documento de identidad del cliente") @RequestParam("documentoIdentidad") String documentoIdentidad) {
        Cliente cliente = clienteRepository.findByDocumentoIdentidad(documentoIdentidad)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cliente no encontrado con documento: " + documentoIdentidad));

        return ResponseEntity.ok(citaService.cancelarCita(idCita, cliente.getId()));
    }
}