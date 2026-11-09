package com.veterinaria.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veterinaria.dto.ActualizarRolesUsuarioDTO;
import com.veterinaria.dto.AuditoriaUsuarioDTO;
import com.veterinaria.dto.CambioEstadoUsuarioDTO;
import com.veterinaria.dto.UsuarioAdminDTO;
import com.veterinaria.model.Rol;
import com.veterinaria.service.AdministracionUsuariosService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** HU19 - Gestionar usuarios. Requiere el rol ADMINISTRADOR (ver SecurityConfig). */
@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
@Tag(name = "Administración de usuarios", description = "Listado, estado y roles de las cuentas (solo administradores)")
public class AdministracionUsuariosController {

    private final AdministracionUsuariosService administracionService;

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Filtra opcionalmente por nombre o correo, rol y estado", responses = {
            @ApiResponse(responseCode = "200", description = "Usuarios ordenados por nombre"),
            @ApiResponse(responseCode = "401", description = "Sin sesión"),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es administrador")
    })
    public ResponseEntity<List<UsuarioAdminDTO>> listar(
            @Parameter(description = "Texto a buscar en nombre o correo") @RequestParam(required = false) String buscar,
            @Parameter(description = "CLIENTE, VETERINARIO o ADMINISTRADOR") @RequestParam(required = false) Rol rol,
            @Parameter(description = "true = activos, false = inactivos") @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(administracionService.listar(buscar, rol, activo));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activar o inactivar una cuenta",
            description = "Inactivar cierra de inmediato las sesiones abiertas del usuario. El cambio queda auditado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Estado actualizado"),
                    @ApiResponse(responseCode = "400", description = "Falta el estado"),
                    @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
                    @ApiResponse(responseCode = "409", description = "Auto-desactivación o la clínica quedaría sin administrador activo")
            })
    public ResponseEntity<UsuarioAdminDTO> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoUsuarioDTO solicitud,
            Authentication authentication) {
        return ResponseEntity.ok(administracionService.cambiarEstado(id, solicitud.activo(), authentication.getName()));
    }

    @PatchMapping("/{id}/roles")
    @Operation(summary = "Actualizar los roles de una cuenta",
            description = "Reemplaza los roles del usuario. El cambio queda auditado y aplica de inmediato.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Roles actualizados"),
                    @ApiResponse(responseCode = "400", description = "Sin roles, rol inválido o rol sin perfil vinculado"),
                    @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
                    @ApiResponse(responseCode = "409", description = "Auto-retiro del rol administrador o la clínica quedaría sin administrador activo")
            })
    public ResponseEntity<UsuarioAdminDTO> actualizarRoles(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarRolesUsuarioDTO solicitud,
            Authentication authentication) {
        return ResponseEntity.ok(administracionService.actualizarRoles(id, solicitud.roles(), authentication.getName()));
    }

    @GetMapping("/{id}/auditoria")
    @Operation(summary = "Historial de cambios de un usuario",
            description = "Qué cambió, qué administrador lo hizo y cuándo, del más reciente al más antiguo")
    public ResponseEntity<List<AuditoriaUsuarioDTO>> consultarAuditoria(@PathVariable Long id) {
        return ResponseEntity.ok(administracionService.consultarAuditoria(id));
    }
}
