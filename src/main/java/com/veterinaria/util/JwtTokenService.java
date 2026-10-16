package com.veterinaria.util;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenService {

    private final SecretKey signingKey;
    private final long expiracionMs;
    private final Clock clock;

    public JwtTokenService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expiracionMs, Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
        this.clock = clock;
    }

    public String generarToken(String subject) {
        Date emitido = Date.from(clock.instant());
        Date expiracion = new Date(emitido.getTime() + expiracionMs);
        return Jwts.builder()
                .subject(subject)
                .issuedAt(emitido)
                .expiration(expiracion)
                .signWith(signingKey)
                .compact();
    }

    public Claims validarYObtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpiracionMs() {
        return expiracionMs;
    }
}