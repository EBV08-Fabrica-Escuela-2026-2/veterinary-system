package com.veterinaria.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.veterinaria.model.Cliente;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.util.JwtTokenService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final ClienteRepository clienteRepository;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, ClienteRepository clienteRepository) {
        this.jwtTokenService = jwtTokenService;
        this.clienteRepository = clienteRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String subject = jwtTokenService.validarYObtenerClaims(authorization.substring(7)).getSubject();
                clienteRepository.findByCorreoIgnoreCase(subject)
                        .filter(cliente -> Boolean.TRUE.equals(cliente.getActivo()))
                        .ifPresent(cliente -> autenticar(cliente));
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(Cliente cliente) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                cliente.getCorreo(), null, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}