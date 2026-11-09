package com.veterinaria.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.veterinaria.model.Rol;
import com.veterinaria.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

class JwtTokenServiceTest {

    private static final String SECRETO = "clave-secreta-de-pruebas-con-longitud-suficiente-2026";
    private static final Instant EMISION = Instant.parse("2026-11-09T15:00:00Z");

    @Test
    void generarToken_incluyeRolesIdentificadorVersionYExpiracion() {
        JwtTokenService servicio = new JwtTokenService(SECRETO, 3_600_000L, Clock.fixed(EMISION, ZoneOffset.UTC));
        Usuario usuario = Usuario.builder()
                .id(7L)
                .correo("ana@vet.com")
                .versionToken(3)
                .roles(new HashSet<>(List.of(Rol.VETERINARIO, Rol.ADMINISTRADOR)))
                .build();

        Claims claims = servicio.validarYObtenerClaims(servicio.generarToken(usuario));

        assertThat(claims.getSubject()).isEqualTo("ana@vet.com");
        assertThat(claims.get(JwtTokenService.CLAIM_USUARIO_ID, Number.class).longValue()).isEqualTo(7L);
        assertThat(claims.get(JwtTokenService.CLAIM_ROLES, List.class)).containsExactly("ADMINISTRADOR", "VETERINARIO");
        assertThat(claims.get(JwtTokenService.CLAIM_ROL, String.class)).isEqualTo("ADMINISTRADOR");
        assertThat(claims.get(JwtTokenService.CLAIM_VERSION_TOKEN, Number.class).intValue()).isEqualTo(3);
        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(EMISION);
        assertThat(claims.getExpiration().toInstant()).isEqualTo(EMISION.plusSeconds(3600));
    }

    @Test
    void validarYObtenerClaims_rechazaUnTokenVencido() {
        JwtTokenService emisor = new JwtTokenService(SECRETO, 60_000L, Clock.fixed(EMISION, ZoneOffset.UTC));
        Usuario usuario = Usuario.builder().id(1L).correo("ana@vet.com")
                .roles(new HashSet<>(List.of(Rol.CLIENTE))).build();
        String token = emisor.generarToken(usuario);

        JwtTokenService validadorDespues = new JwtTokenService(SECRETO, 60_000L,
                Clock.fixed(EMISION.plusSeconds(3600), ZoneOffset.UTC));

        assertThatThrownBy(() -> validadorDespues.validarYObtenerClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
