package com.veterinaria.service.impl;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.dto.AuditoriaUsuarioDTO;
import com.veterinaria.dto.UsuarioAdminDTO;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.AccionAuditoria;
import com.veterinaria.model.Rol;
import com.veterinaria.model.Usuario;
import com.veterinaria.model.UsuarioAuditoria;
import com.veterinaria.repository.UsuarioAuditoriaRepository;
import com.veterinaria.repository.UsuarioRepository;
import com.veterinaria.service.AdministracionUsuariosService;
import com.veterinaria.service.ReglaIntegridadAdministrativa;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdministracionUsuariosServiceImpl implements AdministracionUsuariosService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioAuditoriaRepository auditoriaRepository;
    private final ReglaIntegridadAdministrativa reglaIntegridad;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioAdminDTO> listar(String buscar, Rol rol, Boolean activo) {
        String filtro = buscar == null ? "" : buscar.trim().toLowerCase(Locale.ROOT);
        return usuarioRepository.findAll(Sort.by("nombre").and(Sort.by("id"))).stream()
                .filter(usuario -> filtro.isEmpty() || contiene(usuario.getNombre(), filtro)
                        || contiene(usuario.getCorreo(), filtro))
                .filter(usuario -> rol == null || usuario.tieneRol(rol))
                .filter(usuario -> activo == null || activo == usuario.estaActivo())
                .map(this::aDto)
                .toList();
    }

    @Override
    @Transactional
    public UsuarioAdminDTO cambiarEstado(Long usuarioId, boolean activo, String correoAdministrador) {
        Usuario administrador = obtenerAdministrador(correoAdministrador);
        reglaIntegridad.bloquearAdministradoresActivos();
        Usuario usuario = obtenerParaActualizar(usuarioId);
        reglaIntegridad.validarCambioEstado(administrador, usuario, activo);

        boolean estadoAnterior = usuario.estaActivo();
        if (estadoAnterior == activo) {
            return aDto(usuario);
        }

        usuario.setActivo(activo);
        if (!activo) {
            // Invalida los JWT ya emitidos: la sesión abierta se cierra en la siguiente petición.
            usuario.setVersionToken(usuario.getVersionToken() + 1);
        }
        usuarioRepository.saveAndFlush(usuario);
        reglaIntegridad.verificarAdministradorActivoRestante();

        auditar(usuario, administrador, AccionAuditoria.CAMBIO_ESTADO, textoEstado(estadoAnterior), textoEstado(activo));
        return aDto(usuario);
    }

    @Override
    @Transactional
    public UsuarioAdminDTO actualizarRoles(Long usuarioId, Set<Rol> roles, String correoAdministrador) {
        if (roles == null || roles.isEmpty() || roles.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Selecciona al menos un rol válido");
        }
        Set<Rol> nuevosRoles = EnumSet.copyOf(roles);

        Usuario administrador = obtenerAdministrador(correoAdministrador);
        reglaIntegridad.bloquearAdministradoresActivos();
        Usuario usuario = obtenerParaActualizar(usuarioId);
        reglaIntegridad.validarCambioRoles(administrador, usuario, nuevosRoles);
        validarVinculos(usuario, nuevosRoles);

        Set<Rol> rolesAnteriores = usuario.getRoles().isEmpty()
                ? EnumSet.noneOf(Rol.class)
                : EnumSet.copyOf(usuario.getRoles());
        if (rolesAnteriores.equals(nuevosRoles)) {
            return aDto(usuario);
        }

        usuario.getRoles().clear();
        usuario.getRoles().addAll(nuevosRoles);
        usuarioRepository.saveAndFlush(usuario);
        reglaIntegridad.verificarAdministradorActivoRestante();

        auditar(usuario, administrador, AccionAuditoria.CAMBIO_ROLES, textoRoles(rolesAnteriores),
                textoRoles(nuevosRoles));
        return aDto(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaUsuarioDTO> consultarAuditoria(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }
        return auditoriaRepository.findByUsuarioIdOrderByFechaDescIdDesc(usuarioId).stream()
                .map(registro -> new AuditoriaUsuarioDTO(
                        registro.getId(),
                        registro.getAccion().name(),
                        registro.getValorAnterior(),
                        registro.getValorNuevo(),
                        registro.getAdministrador() == null ? null : registro.getAdministrador().getId(),
                        registro.getAdministrador() == null ? null : registro.getAdministrador().getCorreo(),
                        registro.getFecha()))
                .toList();
    }

    private Usuario obtenerAdministrador(String correo) {
        return usuarioRepository.findByCorreoIgnoreCase(correo)
                .filter(Usuario::estaActivo)
                .filter(usuario -> usuario.tieneRol(Rol.ADMINISTRADOR))
                .orElseThrow(() -> new AccessDeniedException("Se requiere un administrador activo"));
    }

    private Usuario obtenerParaActualizar(Long usuarioId) {
        return usuarioRepository.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private void validarVinculos(Usuario usuario, Set<Rol> roles) {
        if (roles.contains(Rol.VETERINARIO) && usuario.getVeterinario() == null) {
            throw new IllegalArgumentException(
                    "Para asignar el rol VETERINARIO la cuenta debe estar vinculada a un veterinario registrado");
        }
        if (roles.contains(Rol.CLIENTE) && usuario.getCliente() == null) {
            throw new IllegalArgumentException(
                    "Para asignar el rol CLIENTE la cuenta debe estar vinculada a un cliente registrado");
        }
    }

    private void auditar(Usuario usuario, Usuario administrador, AccionAuditoria accion, String anterior,
            String nuevo) {
        auditoriaRepository.save(UsuarioAuditoria.builder()
                .usuario(usuario)
                .administrador(administrador)
                .accion(accion)
                .valorAnterior(anterior)
                .valorNuevo(nuevo)
                .build());
    }

    private UsuarioAdminDTO aDto(Usuario usuario) {
        Rol rolPrincipal = usuario.rolPrincipal();
        return new UsuarioAdminDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                rolPrincipal == null ? null : rolPrincipal.name(),
                usuario.getRoles().stream().sorted().map(Rol::name).toList(),
                usuario.estaActivo(),
                usuario.getCliente() == null ? null : usuario.getCliente().getId(),
                usuario.getVeterinario() == null ? null : usuario.getVeterinario().getId(),
                usuario.getUpdatedAt());
    }

    private static boolean contiene(String valor, String filtro) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(filtro);
    }

    private static String textoEstado(boolean activo) {
        return activo ? "ACTIVO" : "INACTIVO";
    }

    private static String textoRoles(Set<Rol> roles) {
        return roles.stream().sorted().map(Rol::name).collect(Collectors.joining(","));
    }
}
