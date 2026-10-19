package com.veterinaria.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.dto.CancelacionCitaDTO;
import com.veterinaria.dto.CancelacionVeterinarioDTO;
import com.veterinaria.dto.CitaVeterinarioDTO;
import com.veterinaria.dto.DisponibilidadVeterinarioDTO;
import com.veterinaria.dto.FranjaDisponibilidadDTO;
import com.veterinaria.dto.FranjaDisponibilidadRequestDTO;
import com.veterinaria.dto.VeterinarioPortalPerfilDTO;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.service.CitaService;
import com.veterinaria.service.VeterinarioPortalService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/veterinario-portal")
@RequiredArgsConstructor
@Tag(name = "Portal del veterinario", description = "API del veterinario autenticado: perfil, agenda y disponibilidad")
public class VeterinarioPortalController {

    private final VeterinarioRepository veterinarioRepository;
    private final VeterinarioPortalService portalService;
    private final CitaService citaService;

    @GetMapping("/perfil")
    public ResponseEntity<VeterinarioPortalPerfilDTO> obtenerPerfil(Authentication authentication) {
        Veterinario veterinario = obtenerVeterinario(authentication);
        return ResponseEntity.ok(new VeterinarioPortalPerfilDTO(
                veterinario.getId(),
                veterinario.getNombre(),
                veterinario.getCorreo(),
                veterinario.getDocumentoIdentidad(),
                veterinario.getEspecialidad()));
    }

    @GetMapping("/citas")
    public ResponseEntity<List<CitaVeterinarioDTO>> listarCitas(Authentication authentication) {
        Veterinario veterinario = obtenerVeterinario(authentication);
        return ResponseEntity.ok(portalService.listarCitas(veterinario.getId()));
    }

    @PatchMapping("/citas/{idCita}/cancelar")
    public ResponseEntity<CancelacionCitaDTO> cancelarCita(
            @PathVariable Long idCita,
            @Valid @RequestBody CancelacionVeterinarioDTO solicitud,
            Authentication authentication) {
        Veterinario veterinario = obtenerVeterinario(authentication);
        return ResponseEntity.ok(citaService.cancelarCitaPorVeterinario(
                idCita, veterinario.getId(), solicitud.getMotivo()));
    }

    @GetMapping("/disponibilidad")
    @Operation(summary = "Consultar mi disponibilidad", description = "Devuelve los siete días de la semana con las franjas activas del veterinario autenticado", responses = {
            @ApiResponse(responseCode = "200", description = "Disponibilidad consultada exitosamente"),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es un veterinario activo")
    })
    public ResponseEntity<DisponibilidadVeterinarioDTO> consultarDisponibilidad(Authentication authentication) {
        Veterinario veterinario = obtenerVeterinario(authentication);
        return ResponseEntity.ok(portalService.consultarDisponibilidad(veterinario.getId()));
    }

    @PutMapping("/disponibilidad")
    @Operation(summary = "Reemplazar mi disponibilidad", description = "Reemplaza todas las franjas del veterinario autenticado. Los días repetidos se agrupan y los días con activo=false se ignoran", responses = {
            @ApiResponse(responseCode = "200", description = "Disponibilidad guardada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o sin ninguna franja"),
            @ApiResponse(responseCode = "409", description = "Hay franjas que se traslapan en un mismo día")
    })
    public ResponseEntity<DisponibilidadVeterinarioDTO> guardarDisponibilidad(
            @Valid @RequestBody DisponibilidadVeterinarioDTO solicitud,
            Authentication authentication) {
        Veterinario veterinario = obtenerVeterinario(authentication);
        return ResponseEntity.ok(portalService.guardarDisponibilidad(veterinario, solicitud));
    }

    @PostMapping("/disponibilidad/franjas")
    @Operation(summary = "Agregar una franja de disponibilidad", description = "Crea una franja para el veterinario autenticado sin tocar las demás", responses = {
            @ApiResponse(responseCode = "201", description = "Franja creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Día u horas inválidos"),
            @ApiResponse(responseCode = "409", description = "La franja se traslapa con otra franja activa del mismo día")
    })
    public ResponseEntity<FranjaDisponibilidadDTO> crearFranja(
            @Valid @RequestBody FranjaDisponibilidadRequestDTO solicitud,
            Authentication authentication) {
        Veterinario veterinario = obtenerVeterinario(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.crearFranja(veterinario, solicitud));
    }

    @PutMapping("/disponibilidad/franjas/{franjaId}")
    @Operation(summary = "Editar una franja de disponibilidad", description = "Actualiza día y horas de una franja propia del veterinario autenticado", responses = {
            @ApiResponse(responseCode = "200", description = "Franja actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Día u horas inválidos"),
            @ApiResponse(responseCode = "404", description = "La franja no existe o no pertenece al veterinario"),
            @ApiResponse(responseCode = "409", description = "La franja se traslapa con otra franja activa del mismo día")
    })
    public ResponseEntity<FranjaDisponibilidadDTO> actualizarFranja(
            @PathVariable Long franjaId,
            @Valid @RequestBody FranjaDisponibilidadRequestDTO solicitud,
            Authentication authentication) {
        Veterinario veterinario = obtenerVeterinario(authentication);
        return ResponseEntity.ok(portalService.actualizarFranja(veterinario.getId(), franjaId, solicitud));
    }

    @DeleteMapping("/disponibilidad/franjas/{franjaId}")
    @Operation(summary = "Eliminar una franja de disponibilidad", description = "Elimina una franja propia del veterinario autenticado", responses = {
            @ApiResponse(responseCode = "200", description = "Franja eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "La franja no existe o no pertenece al veterinario")
    })
    public ResponseEntity<Map<String, String>> eliminarFranja(
            @PathVariable Long franjaId,
            Authentication authentication) {
        Veterinario veterinario = obtenerVeterinario(authentication);
        portalService.eliminarFranja(veterinario.getId(), franjaId);
        return ResponseEntity.ok(Map.of("mensaje", "Franja eliminada correctamente"));
    }

    private Veterinario obtenerVeterinario(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Se requiere iniciar sesión como veterinario");
        }
        return veterinarioRepository.findByCorreoIgnoreCase(authentication.getName())
                .filter(item -> Boolean.TRUE.equals(item.getActivo()))
                .orElseThrow(() -> new AccessDeniedException(
                        "El usuario autenticado no corresponde a un veterinario activo"));
    }
}
