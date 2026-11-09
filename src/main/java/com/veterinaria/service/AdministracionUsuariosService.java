package com.veterinaria.service;

import java.util.List;
import java.util.Set;

import com.veterinaria.dto.AuditoriaUsuarioDTO;
import com.veterinaria.dto.UsuarioAdminDTO;
import com.veterinaria.model.Rol;

public interface AdministracionUsuariosService {

    /** Lista los usuarios, opcionalmente filtrados por texto (nombre o correo), rol y estado. */
    List<UsuarioAdminDTO> listar(String buscar, Rol rol, Boolean activo);

    /** Activa o inactiva una cuenta. Inactivarla cierra de inmediato sus sesiones abiertas. */
    UsuarioAdminDTO cambiarEstado(Long usuarioId, boolean activo, String correoAdministrador);

    UsuarioAdminDTO actualizarRoles(Long usuarioId, Set<Rol> roles, String correoAdministrador);

    List<AuditoriaUsuarioDTO> consultarAuditoria(Long usuarioId);
}
