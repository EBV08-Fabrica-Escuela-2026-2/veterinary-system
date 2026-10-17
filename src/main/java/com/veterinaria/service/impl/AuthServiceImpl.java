package com.veterinaria.service.impl;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.dto.AuthResponseDTO;
import com.veterinaria.dto.ClienteResponseDTO;
import com.veterinaria.dto.LoginAuthDTO;
import com.veterinaria.dto.RegistroAuthDTO;
import com.veterinaria.exception.ClienteNoAutenticadoException;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.Cliente;
import com.veterinaria.model.ClienteCredencial;
import com.veterinaria.repository.ClienteCredencialRepository;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.service.AuthService;
import com.veterinaria.util.JwtTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ClienteRepository clienteRepository;
    private final ClienteCredencialRepository credencialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Override
    @Transactional
    public AuthResponseDTO registrar(RegistroAuthDTO solicitud) {
        String correo = solicitud.getCorreo().trim();
        String documento = solicitud.getDocumentoIdentidad().trim();
        Cliente cliente = clienteRepository.findByCorreoIgnoreCaseAndDocumentoIdentidad(correo, documento)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un cliente con ese correo y documento de identidad"));

        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new AccessDeniedException("El cliente no se encuentra activo");
        }
        if (credencialRepository.existsByClienteId(cliente.getId())) {
            throw new IllegalArgumentException("El cliente ya tiene una cuenta registrada");
        }

        ClienteCredencial credencial = ClienteCredencial.builder()
                .cliente(cliente)
                .passwordHash(passwordEncoder.encode(solicitud.getPassword()))
                .build();
        credencialRepository.save(credencial);

        return new AuthResponseDTO("Cuenta registrada correctamente", null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO iniciarSesion(LoginAuthDTO solicitud) {
        Cliente cliente = clienteRepository.findByCorreoIgnoreCase(solicitud.getCorreo().trim())
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .orElseThrow(this::credencialesInvalidas);
        ClienteCredencial credencial = credencialRepository.findByClienteId(cliente.getId())
                .orElseThrow(this::credencialesInvalidas);

        if (!passwordEncoder.matches(solicitud.getPassword(), credencial.getPasswordHash())) {
            throw credencialesInvalidas();
        }

        String token = jwtTokenService.generarToken(cliente.getCorreo());
        return new AuthResponseDTO("Inicio de sesión exitoso", token, "Bearer",
                jwtTokenService.getExpiracionMs());
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerPerfil(String correo) {
        Cliente cliente = clienteRepository.findByCorreoIgnoreCase(correo)
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .orElseThrow(this::credencialesInvalidas);

        return ClienteResponseDTO.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .documentoIdentidad(cliente.getDocumentoIdentidad())
                .telefono(cliente.getTelefono())
                .correo(cliente.getCorreo())
                .direccion(cliente.getDireccion())
                .activo(cliente.getActivo())
                .build();
    }

    private ClienteNoAutenticadoException credencialesInvalidas() {
        return new ClienteNoAutenticadoException("Correo o contraseña incorrectos");
    }
}
