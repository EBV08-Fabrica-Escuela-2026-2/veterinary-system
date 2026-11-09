package com.veterinaria.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Agrega la cabecera Content-Language: es-CO a todas las respuestas HTTP para
 * orientar la pronunciación de los sintetizadores de voz.
 *
 * Corre con la máxima prioridad (antes de Spring Security) para que también
 * la lleven los errores 401 y 403 que genera la seguridad.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContentLanguageFilter extends OncePerRequestFilter {

    public static final String IDIOMA = "es-CO";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader("Content-Language", IDIOMA);
        filterChain.doFilter(request, response);
    }
}
