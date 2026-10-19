package com.veterinaria.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.dto.CancelacionCitaDTO;
import com.veterinaria.dto.CancelacionVeterinarioDTO;
import com.veterinaria.dto.CitaVeterinarioDTO;
import com.veterinaria.dto.DisponibilidadVeterinarioDTO;
import com.veterinaria.dto.VeterinarioPortalPerfilDTO;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.service.CitaService;
import com.veterinaria.service.VeterinarioPortalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/veterinario-portal")
@RequiredArgsConstructor
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
    public ResponseEntity<DisponibilidadVeterinarioDTO> consultarDisponibilidad(Authentication authentication) {
        Veterinario veterinario = obtenerVeterinario(authentication);
        return ResponseEntity.ok(portalService.consultarDisponibilidad(veterinario.getId()));
    }

    @PutMapping("/disponibilidad")
    public ResponseEntity<DisponibilidadVeterinarioDTO> guardarDisponibilidad(
            @RequestBody DisponibilidadVeterinarioDTO solicitud,
            Authentication authentication) {
        Veterinario veterinario = obtenerVeterinario(authentication);
        return ResponseEntity.ok(portalService.guardarDisponibilidad(veterinario, solicitud));
    }

    @DeleteMapping("/disponibilidad/franjas/{franjaId}")
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
