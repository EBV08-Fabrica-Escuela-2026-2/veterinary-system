package com.veterinaria.controller;

import com.veterinaria.dto.DisponibilidadDiaDTO;
import com.veterinaria.service.DisponibilidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/citas/disponibilidad")
@RequiredArgsConstructor
@Tag(name = "Disponibilidad", description = "API para consultar horarios disponibles")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    @GetMapping
    @Operation(summary = "Consultar disponibilidad de un día", description = "Calcula las franjas horarias libres de un servicio para la fecha indicada, opcionalmente filtrando por veterinario", responses = {
            @ApiResponse(responseCode = "200", description = "Disponibilidad consultada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Fecha inválida, servicio inactivo o servicio que no pertenece al veterinario indicado"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    public ResponseEntity<DisponibilidadDiaDTO> consultar(
            @Parameter(description = "Id del servicio") @RequestParam Long servicioId,
            @Parameter(description = "Fecha a consultar en formato yyyy-MM-dd") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(description = "Id del veterinario (opcional)") @RequestParam(required = false) Long veterinarioId) {
        return ResponseEntity.ok(disponibilidadService.consultarDisponibilidad(servicioId, veterinarioId, fecha));
    }

    @GetMapping("/rango")
    @Operation(summary = "Consultar disponibilidad de un rango de fechas", description = "Calcula la disponibilidad diaria de un servicio entre dos fechas (máximo 31 días), opcionalmente filtrando por veterinario", responses = {
            @ApiResponse(responseCode = "200", description = "Disponibilidad consultada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Fechas inválidas, rango mayor a 31 días, servicio inactivo o servicio que no pertenece al veterinario indicado"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    public ResponseEntity<List<DisponibilidadDiaDTO>> consultarRango(
            @Parameter(description = "Id del servicio") @RequestParam Long servicioId,
            @Parameter(description = "Fecha inicial en formato yyyy-MM-dd") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "Fecha final en formato yyyy-MM-dd") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @Parameter(description = "Id del veterinario (opcional)") @RequestParam(required = false) Long veterinarioId) {
        return ResponseEntity
                .ok(disponibilidadService.consultarDisponibilidadRango(servicioId, veterinarioId, fechaInicio, fechaFin));
    }
}
