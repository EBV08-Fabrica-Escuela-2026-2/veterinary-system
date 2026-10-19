package com.veterinaria.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.veterinaria.model.Cliente;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.util.JwtTokenService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final ClienteRepository clienteRepository;
    private final VeterinarioRepository veterinarioRepository;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, ClienteRepository clienteRepository,
            VeterinarioRepository veterinarioRepository) {
        this.jwtTokenService = jwtTokenService;
        this.clienteRepository = clienteRepository;
        this.veterinarioRepository = veterinarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String subject = jwtTokenService.validarYObtenerClaims(authorization.substring(7)).getSubject();
                boolean clienteAutenticado = clienteRepository.findByCorreoIgnoreCase(subject)
                        .filter(cliente -> Boolean.TRUE.equals(cliente.getActivo()))
                        .map(cliente -> {
                            autenticar(cliente);
                            return true;
                        })
                        .orElse(false);
                if (!clienteAutenticado) {
                    veterinarioRepository.findByCorreoIgnoreCase(subject)
                            .filter(veterinario -> Boolean.TRUE.equals(veterinario.getActivo()))
                            .ifPresent(this::autenticar);
                }
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

    private void autenticar(Veterinario veterinario) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                veterinario.getCorreo(), null, List.of(new SimpleGrantedAuthority("ROLE_VETERINARIO")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
