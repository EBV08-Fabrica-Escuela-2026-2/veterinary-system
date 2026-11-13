package com.veterinaria.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.dto.OcupacionResumenDTO;
import com.veterinaria.service.OcupacionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ocupacion")
@RequiredArgsConstructor
@Tag(name = "Ocupación", description = "Consultas de ocupación de la clínica por período")
public class OcupacionController {

    private final OcupacionService ocupacionService;

    @GetMapping("/resumen")
    @Operation(summary = "Resumen de ocupación", description = "Indicadores generales del período y profesionales con mayor/menor demanda",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Resumen devuelto correctamente"),
                    @ApiResponse(responseCode = "400", description = "Parámetros inválidos"),
                    @ApiResponse(responseCode = "403", description = "El usuario no tiene permisos administrativos")
            })
    public ResponseEntity<OcupacionResumenDTO> consultarResumen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long veterinarioId,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(defaultValue = "DIA") String agrupacion) {
        return ResponseEntity.ok(ocupacionService.consultarResumen(fechaInicio, fechaFin, veterinarioId, sedeId, agrupacion));
    }

    @GetMapping("/distribucion")
    @Operation(summary = "Distribución por período", description = "Agrupa la ocupación por día, semana o mes según la agrupación solicitada",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Distribución devuelta correctamente"),
                    @ApiResponse(responseCode = "400", description = "Parámetros inválidos"),
                    @ApiResponse(responseCode = "403", description = "El usuario no tiene permisos administrativos")
            })
    public ResponseEntity<OcupacionResumenDTO> consultarDistribucion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long veterinarioId,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(defaultValue = "DIA") String agrupacion) {
        return ResponseEntity.ok(ocupacionService.consultarDistribucion(fechaInicio, fechaFin, veterinarioId, sedeId, agrupacion));
    }
}
