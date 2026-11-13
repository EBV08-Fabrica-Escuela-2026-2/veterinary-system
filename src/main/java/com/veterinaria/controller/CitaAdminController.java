package com.veterinaria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.dto.ActualizarEstadoCitaRequestDTO;
import com.veterinaria.dto.CancelacionCitaDTO;
import com.veterinaria.dto.CancelacionVeterinarioDTO;
import com.veterinaria.dto.EstadoCitaResponseDTO;
import com.veterinaria.service.CitaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/citas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Citas (administración)", description = "Acciones del administrador sobre cualquier cita")
public class CitaAdminController {

    private final CitaService citaService;

    @PatchMapping("/{idCita}/cancelar")
    @Operation(summary = "Cancelar una cita como administrador",
            description = "Motivo obligatorio y decisión sobre el horario (LIBERAR_CUPO o BLOQUEAR_FRANJA). Solo ADMINISTRADOR.")
    public ResponseEntity<CancelacionCitaDTO> cancelar(@PathVariable Long idCita,
            @Valid @RequestBody CancelacionVeterinarioDTO solicitud, Authentication authentication) {
        return ResponseEntity.ok(citaService.cancelarCitaPorProfesional(
                idCita, null, authentication.getName(), solicitud.getMotivo(), solicitud.getDecisionHorario()));
    }

    @PatchMapping("/{idCita}/estado")
    @Operation(summary = "Registrar el resultado de una cita como administrador",
            description = "Pasa una cita CONFIRMADA ya terminada a COMPLETADA o NO_ASISTIO. Solo ADMINISTRADOR.")
    public ResponseEntity<EstadoCitaResponseDTO> actualizarEstado(@PathVariable Long idCita,
            @Valid @RequestBody ActualizarEstadoCitaRequestDTO solicitud) {
        return ResponseEntity.ok(citaService.cerrarCitaPorVeterinario(idCita, null, solicitud.estado()));
    }
}