package com.veterinaria.config;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.veterinaria.model.Usuario;
import com.veterinaria.repository.UsuarioRepository;
import com.veterinaria.util.JwtTokenService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica cada petición con el JWT del encabezado Authorization.
 * Los roles y el estado se leen de la base de datos en cada petición, de modo que
 * desactivar una cuenta o cambiar sus roles tiene efecto inmediato. Si la versión
 * de token del JWT no coincide con la del usuario, el token se considera revocado.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UsuarioRepository usuarioRepository) {
        this.jwtTokenService = jwtTokenService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtTokenService.validarYObtenerClaims(authorization.substring(7));
                int versionToken = leerVersionToken(claims);
                usuarioRepository.findByCorreoIgnoreCase(claims.getSubject())
                        .filter(Usuario::estaActivo)
                        .filter(usuario -> Objects.equals(usuario.getVersionToken(), versionToken))
                        .ifPresent(this::autenticar);
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private int leerVersionToken(Claims claims) {
        Object valor = claims.get(JwtTokenService.CLAIM_VERSION_TOKEN);
        return valor instanceof Number numero ? numero.intValue() : 0;
    }

    private void autenticar(Usuario usuario) {
        List<SimpleGrantedAuthority> permisos = usuario.getRoles().stream()
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.name()))
                .toList();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                usuario.getCorreo(), null, permisos);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
