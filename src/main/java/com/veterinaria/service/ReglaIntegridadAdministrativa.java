package com.veterinaria.service;

import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.veterinaria.exception.IntegridadAdministrativaException;
import com.veterinaria.model.Rol;
import com.veterinaria.model.Usuario;
import com.veterinaria.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Regla de negocio de integridad administrativa (TSK-19-04): la clínica nunca puede
 * quedar sin al menos un administrador activo, y un administrador no puede
 * desactivarse ni retirarse el rol de administrador a sí mismo.
 *
 * <p>Debe usarse dentro de una transacción: {@link #bloquearAdministradoresActivos()}
 * bloquea las filas de los administradores activos (siempre en el mismo orden) para que
 * dos administradores no puedan desactivarse mutuamente al mismo tiempo.
 */
@Component
@RequiredArgsConstructor
public class ReglaIntegridadAdministrativa {

    public static final String SIN_ADMINISTRADOR =
            "No es posible dejar la clínica sin al menos un administrador activo";
    public static final String AUTO_DESACTIVACION =
            "No puedes desactivar tu propia cuenta. Pide a otro administrador que lo haga";
    public static final String AUTO_RETIRO_ROL =
            "No puedes retirarte tu propio rol de administrador. Pide a otro administrador que lo haga";

    private final UsuarioRepository usuarioRepository;

    public void bloquearAdministradoresActivos() {
        for (Long id : usuarioRepository.findIdsActivosConRol(Rol.ADMINISTRADOR)) {
            usuarioRepository.findByIdForUpdate(id);
        }
    }

    public void validarCambioEstado(Usuario administrador, Usuario objetivo, boolean nuevoActivo) {
        if (nuevoActivo || !esElMismo(administrador, objetivo)) {
            return;
        }
        long otrosAdministradoresActivos = usuarioRepository.contarActivosConRol(Rol.ADMINISTRADOR)
                - (objetivo.estaActivo() && objetivo.tieneRol(Rol.ADMINISTRADOR) ? 1 : 0);
        throw new IntegridadAdministrativaException(
                otrosAdministradoresActivos <= 0 ? SIN_ADMINISTRADOR : AUTO_DESACTIVACION);
    }

    public void validarCambioRoles(Usuario administrador, Usuario objetivo, Set<Rol> nuevosRoles) {
        if (esElMismo(administrador, objetivo) && objetivo.tieneRol(Rol.ADMINISTRADOR)
                && !nuevosRoles.contains(Rol.ADMINISTRADOR)) {
            throw new IntegridadAdministrativaException(AUTO_RETIRO_ROL);
        }
    }

    /** Se llama después de aplicar (y sincronizar) el cambio; si no queda ningún administrador activo, lo revierte. */
    public void verificarAdministradorActivoRestante() {
        if (usuarioRepository.contarActivosConRol(Rol.ADMINISTRADOR) == 0) {
            throw new IntegridadAdministrativaException(SIN_ADMINISTRADOR);
        }
    }

    private boolean esElMismo(Usuario administrador, Usuario objetivo) {
        return administrador != null && objetivo != null && Objects.equals(administrador.getId(), objetivo.getId());
    }
}
