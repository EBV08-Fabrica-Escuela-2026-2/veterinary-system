package com.veterinaria.controller;

import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.dto.UsoServiciosReporteDTO;
import com.veterinaria.service.ReporteServicioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Reportes administrativos de gestión clínica")
public class ReporteController {

    private final ReporteServicioService reporteServicioService;

    @GetMapping("/uso-servicios")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Consultar uso de servicios", description = "Consolidado mensual de los servicios clínicos: citas solicitadas, atenciones realizadas (COMPLETADA), valor facturado, tendencia semanal y ranking. Solo administradores.", responses = {
            @ApiResponse(responseCode = "200", description = "Reporte generado"),
            @ApiResponse(responseCode = "400", description = "Mes con formato inválido o servicio inexistente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No es administrador")
    })
    public ResponseEntity<UsoServiciosReporteDTO> usoDeServicios(
            @Parameter(description = "Mes con formato AAAA-MM. Por defecto, el mes en curso", example = "2026-11")
            @RequestParam(value = "mes", required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @Parameter(description = "Nombre del servicio clínico. Vacío = todos los servicios")
            @RequestParam(value = "servicio", required = false) String servicio) {
        return ResponseEntity.ok(reporteServicioService.usoDeServicios(mes, servicio));
    }
}