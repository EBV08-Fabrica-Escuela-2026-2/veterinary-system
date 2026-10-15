package com.veterinaria.controller;

import com.veterinaria.dto.CancelacionCitaDTO;
import com.veterinaria.dto.CitaClienteDTO;
import com.veterinaria.dto.CitaRegistroDTO;
import com.veterinaria.dto.DisponibilidadDiaDTO;
import com.veterinaria.dto.ReprogramacionCitaResponseDTO;
import com.veterinaria.dto.ReprogramarCitaRequestDTO;
import com.veterinaria.exception.ClienteNoAutenticadoException;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.Cita;
import com.veterinaria.model.Cliente;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.service.CitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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
@RequestMapping("/api/citas")
@RequiredArgsConstructor
@Tag(name = "Citas", description = "API para reservar citas veterinarias")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
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

        @GetMapping("/{idCita}/disponibilidad")
        @Operation(summary = "Consultar disponibilidad para reprogramar una cita")
        @SecurityRequirement(name = "bearerAuth")
        public ResponseEntity<DisponibilidadDiaDTO> consultarDisponibilidadReprogramacion(
                        @PathVariable Long idCita,
                        @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                        Authentication authentication) {
                Cliente cliente = obtenerClienteAutenticado(authentication);
                return ResponseEntity.ok(
                                citaService.consultarDisponibilidadReprogramacion(idCita, cliente.getId(), fecha));
        }

        @org.springframework.web.bind.annotation.PatchMapping("/{idCita}/reprogramar")
        @Operation(summary = "Reprogramar una cita propia")
        @SecurityRequirement(name = "bearerAuth")
        public ResponseEntity<ReprogramacionCitaResponseDTO> reprogramarCita(
                        @PathVariable Long idCita,
                        @Valid @RequestBody ReprogramarCitaRequestDTO solicitud,
                        Authentication authentication) {
                Cliente cliente = obtenerClienteAutenticado(authentication);
                return ResponseEntity.ok(citaService.reprogramarCita(idCita, cliente.getId(), solicitud));
        }

        private Cliente obtenerClienteAutenticado(Authentication authentication) {
                if (authentication == null || !authentication.isAuthenticated()
                                || authentication instanceof AnonymousAuthenticationToken) {
                        throw new ClienteNoAutenticadoException("Se requiere iniciar sesión como cliente");
                }

                Cliente cliente = clienteRepository.findByCorreo(authentication.getName())
                                .orElseThrow(() -> new AccessDeniedException("El usuario autenticado no corresponde a un cliente"));
                if (Boolean.FALSE.equals(cliente.getActivo())) {
                        throw new AccessDeniedException("El cliente no se encuentra activo");
                }
                return cliente;
        }
}