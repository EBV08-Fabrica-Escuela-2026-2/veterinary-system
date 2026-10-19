package com.veterinaria.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.util.JwtTokenService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, JwtTokenService jwtTokenService,
            ClienteRepository clienteRepository, VeterinarioRepository veterinarioRepository) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers("/api/veterinario-portal/**").hasRole("VETERINARIO")
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/catalogo/**").permitAll()
                        .requestMatchers("/api/clientes/**").permitAll()
                        .requestMatchers("/api/veterinarios/**").permitAll()
                        .requestMatchers("/api/servicios/**").permitAll()
                        .requestMatchers("/api/mascotas/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/citas").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/citas/*/disponibilidad").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/*/reprogramar").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/*/cancelar-veterinario").hasRole("VETERINARIO")
                        .requestMatchers("/api/citas/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\": \"No autenticado\", \"mensaje\": \"Se requiere iniciar sesión\"}");
                        }))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenService, clienteRepository, veterinarioRepository),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(parseAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> parseAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }
}
