package com.veterinaria.util;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.veterinaria.model.Rol;
import com.veterinaria.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenService {

    public static final String CLAIM_USUARIO_ID = "uid";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_VERSION_TOKEN = "tv";

    private final SecretKey signingKey;
    private final long expiracionMs;
    private final Clock clock;

    public JwtTokenService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expiracionMs, Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
        this.clock = clock;
    }

    /**
     * Emite un JWT firmado con el correo como subject y los claims de rol,
     * identificador de usuario, versión de token (para cierre de sesión forzado)
     * y la expiración configurada.
     */
    public String generarToken(Usuario usuario) {
        Date emitido = Date.from(clock.instant());
        Date expiracion = new Date(emitido.getTime() + expiracionMs);
        List<String> roles = usuario.getRoles().stream().sorted().map(Rol::name).toList();
        Rol rolPrincipal = usuario.rolPrincipal();
        return Jwts.builder()
                .subject(usuario.getCorreo())
                .claim(CLAIM_USUARIO_ID, usuario.getId())
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_ROL, rolPrincipal == null ? null : rolPrincipal.name())
                .claim(CLAIM_VERSION_TOKEN, usuario.getVersionToken())
                .issuedAt(emitido)
                .expiration(expiracion)
                .signWith(signingKey)
                .compact();
    }

    public Claims validarYObtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpiracionMs() {
        return expiracionMs;
    }
}
