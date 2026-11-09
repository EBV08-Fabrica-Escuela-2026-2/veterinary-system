package com.veterinaria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import com.veterinaria.dto.UsuarioAdminDTO;
import com.veterinaria.exception.IntegridadAdministrativaException;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.AccionAuditoria;
import com.veterinaria.model.Rol;
import com.veterinaria.model.Usuario;
import com.veterinaria.model.UsuarioAuditoria;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.UsuarioAuditoriaRepository;
import com.veterinaria.repository.UsuarioRepository;
import com.veterinaria.service.impl.AdministracionUsuariosServiceImpl;

@ExtendWith(MockitoExtension.class)
class AdministracionUsuariosServiceImplTest {

    private static final String CORREO_ADMIN = "admin@vetagenda.com";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioAuditoriaRepository auditoriaRepository;

    private AdministracionUsuariosServiceImpl servicio;

    private Usuario administrador;

    @BeforeEach
    void preparar() {
        ReglaIntegridadAdministrativa regla = new ReglaIntegridadAdministrativa(usuarioRepository);
        servicio = new AdministracionUsuariosServiceImpl(usuarioRepository, auditoriaRepository, regla);
        administrador = usuario(1L, CORREO_ADMIN, Rol.ADMINISTRADOR);
    }

    @Test
    void cambiarEstado_desactivaAlVeterinarioCierraSusSesionesYAuditaElCambio() {
        Usuario veterinario = usuario(2L, "laura@vet.com", Rol.VETERINARIO);
        prepararAdministradorYObjetivo(veterinario);
        when(usuarioRepository.contarActivosConRol(Rol.ADMINISTRADOR)).thenReturn(1L);

        UsuarioAdminDTO resultado = servicio.cambiarEstado(2L, false, CORREO_ADMIN);

        assertThat(resultado.activo()).isFalse();
        assertThat(veterinario.getVersionToken()).isEqualTo(1);
        ArgumentCaptor<UsuarioAuditoria> captor = ArgumentCaptor.forClass(UsuarioAuditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        UsuarioAuditoria registro = captor.getValue();
        assertThat(registro.getAccion()).isEqualTo(AccionAuditoria.CAMBIO_ESTADO);
        assertThat(registro.getAdministrador()).isSameAs(administrador);
        assertThat(registro.getUsuario()).isSameAs(veterinario);
        assertThat(registro.getValorAnterior()).isEqualTo("ACTIVO");
        assertThat(registro.getValorNuevo()).isEqualTo("INACTIVO");
    }

    @Test
    void cambiarEstado_reactivarNoCambiaLaVersionDeToken() {
        Usuario cliente = usuario(3L, "ana@gmail.com", Rol.CLIENTE);
        cliente.setActivo(false);
        cliente.setVersionToken(4);
        prepararAdministradorYObjetivo(cliente);
        when(usuarioRepository.contarActivosConRol(Rol.ADMINISTRADOR)).thenReturn(1L);

        assertThat(servicio.cambiarEstado(3L, true, CORREO_ADMIN).activo()).isTrue();
        assertThat(cliente.getVersionToken()).isEqualTo(4);
    }

    @Test
    void cambiarEstado_sinCambiosNoAuditaNada() {
        Usuario cliente = usuario(3L, "ana@gmail.com", Rol.CLIENTE);
        prepararAdministradorYObjetivo(cliente);

        servicio.cambiarEstado(3L, true, CORREO_ADMIN);

        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_elUnicoAdministradorNoPuedeDesactivarse() {
        prepararAdministradorYObjetivo(administrador);
        when(usuarioRepository.contarActivosConRol(Rol.ADMINISTRADOR)).thenReturn(1L);

        assertThatThrownBy(() -> servicio.cambiarEstado(1L, false, CORREO_ADMIN))
                .isInstanceOf(IntegridadAdministrativaException.class)
                .hasMessage(ReglaIntegridadAdministrativa.SIN_ADMINISTRADOR);

        assertThat(administrador.estaActivo()).isTrue();
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_unAdministradorNoPuedeDesactivarseAunqueHayaOtros() {
        prepararAdministradorYObjetivo(administrador);
        when(usuarioRepository.contarActivosConRol(Rol.ADMINISTRADOR)).thenReturn(2L);

        assertThatThrownBy(() -> servicio.cambiarEstado(1L, false, CORREO_ADMIN))
                .isInstanceOf(IntegridadAdministrativaException.class)
                .hasMessage(ReglaIntegridadAdministrativa.AUTO_DESACTIVACION);
    }

    @Test
    void cambiarEstado_revierteSiNoQuedaNingunAdministradorActivo() {
        Usuario otroAdmin = usuario(5L, "otro@vetagenda.com", Rol.ADMINISTRADOR);
        prepararAdministradorYObjetivo(otroAdmin);
        when(usuarioRepository.contarActivosConRol(Rol.ADMINISTRADOR)).thenReturn(0L);

        assertThatThrownBy(() -> servicio.cambiarEstado(5L, false, CORREO_ADMIN))
                .isInstanceOf(IntegridadAdministrativaException.class)
                .hasMessage(ReglaIntegridadAdministrativa.SIN_ADMINISTRADOR);
        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_bloqueaLosAdministradoresActivosAntesDeValidar() {
        Usuario cliente = usuario(3L, "ana@gmail.com", Rol.CLIENTE);
        prepararAdministradorYObjetivo(cliente);
        when(usuarioRepository.findIdsActivosConRol(Rol.ADMINISTRADOR)).thenReturn(List.of(1L, 7L));
        when(usuarioRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(administrador));
        when(usuarioRepository.findByIdForUpdate(7L)).thenReturn(Optional.empty());
        when(usuarioRepository.contarActivosConRol(Rol.ADMINISTRADOR)).thenReturn(2L);

        servicio.cambiarEstado(3L, false, CORREO_ADMIN);

        verify(usuarioRepository).findByIdForUpdate(1L);
        verify(usuarioRepository).findByIdForUpdate(7L);
    }

    @Test
    void cambiarEstado_usuarioInexistenteResponde404() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO_ADMIN)).thenReturn(Optional.of(administrador));
        when(usuarioRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cambiarEstado(99L, false, CORREO_ADMIN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cambiarEstado_exigeUnAdministradorActivo() {
        Usuario cliente = usuario(3L, "ana@gmail.com", Rol.CLIENTE);
        when(usuarioRepository.findByCorreoIgnoreCase("ana@gmail.com")).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> servicio.cambiarEstado(1L, false, "ana@gmail.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void actualizarRoles_asignaAdministradorAdicionalYAuditaRolesAnterioresYNuevos() {
        Usuario veterinario = usuario(2L, "laura@vet.com", Rol.VETERINARIO);
        veterinario.setVeterinario(Veterinario.builder().id(20L).build());
        prepararAdministradorYObjetivo(veterinario);
        when(usuarioRepository.contarActivosConRol(Rol.ADMINISTRADOR)).thenReturn(2L);

        UsuarioAdminDTO resultado = servicio.actualizarRoles(2L,
                Set.of(Rol.VETERINARIO, Rol.ADMINISTRADOR), CORREO_ADMIN);

        assertThat(resultado.roles()).containsExactly("ADMINISTRADOR", "VETERINARIO");
        assertThat(resultado.rol()).isEqualTo("ADMINISTRADOR");
        ArgumentCaptor<UsuarioAuditoria> captor = ArgumentCaptor.forClass(UsuarioAuditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getAccion()).isEqualTo(AccionAuditoria.CAMBIO_ROLES);
        assertThat(captor.getValue().getValorAnterior()).isEqualTo("VETERINARIO");
        assertThat(captor.getValue().getValorNuevo()).isEqualTo("ADMINISTRADOR,VETERINARIO");
        assertThat(captor.getValue().getAdministrador()).isSameAs(administrador);
    }

    @Test
    void actualizarRoles_unAdministradorNoPuedeRetirarseSuPropioRol() {
        prepararAdministradorYObjetivo(administrador);

        assertThatThrownBy(() -> servicio.actualizarRoles(1L, Set.of(Rol.CLIENTE), CORREO_ADMIN))
                .isInstanceOf(IntegridadAdministrativaException.class)
                .hasMessage(ReglaIntegridadAdministrativa.AUTO_RETIRO_ROL);
        assertThat(administrador.getRoles()).containsExactly(Rol.ADMINISTRADOR);
    }

    @Test
    void actualizarRoles_rechazaRolVeterinarioSinPerfilVinculado() {
        Usuario cliente = usuario(3L, "ana@gmail.com", Rol.ADMINISTRADOR);
        prepararAdministradorYObjetivo(cliente);

        assertThatThrownBy(() -> servicio.actualizarRoles(3L, Set.of(Rol.VETERINARIO), CORREO_ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vinculada a un veterinario");
        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void actualizarRoles_rechazaUnaListaVacia() {
        assertThatThrownBy(() -> servicio.actualizarRoles(3L, Set.of(), CORREO_ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listar_filtraPorTextoRolYEstado() {
        Usuario laura = usuario(2L, "laura@vet.com", Rol.VETERINARIO);
        laura.setNombre("Laura Gómez");
        Usuario ana = usuario(3L, "ana@gmail.com", Rol.CLIENTE);
        ana.setNombre("Ana Ruiz");
        ana.setActivo(false);
        when(usuarioRepository.findAll(any(Sort.class))).thenReturn(List.of(administrador, ana, laura));

        assertThat(servicio.listar("GÓMEZ", null, null)).extracting(UsuarioAdminDTO::id).containsExactly(2L);
        assertThat(servicio.listar("gmail", null, null)).extracting(UsuarioAdminDTO::id).containsExactly(3L);
        assertThat(servicio.listar(null, Rol.ADMINISTRADOR, null)).extracting(UsuarioAdminDTO::id).containsExactly(1L);
        assertThat(servicio.listar(" ", null, false)).extracting(UsuarioAdminDTO::id).containsExactly(3L);
    }

    private void prepararAdministradorYObjetivo(Usuario objetivo) {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO_ADMIN)).thenReturn(Optional.of(administrador));
        when(usuarioRepository.findByIdForUpdate(objetivo.getId())).thenReturn(Optional.of(objetivo));
    }

    private Usuario usuario(Long id, String correo, Rol rol) {
        return Usuario.builder()
                .id(id)
                .nombre("Usuario " + id)
                .correo(correo)
                .passwordHash("hash")
                .activo(true)
                .versionToken(0)
                .roles(EnumSet.of(rol))
                .build();
    }
}
