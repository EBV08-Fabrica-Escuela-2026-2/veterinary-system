package com.veterinaria.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import com.veterinaria.model.Rol;
import com.veterinaria.model.Usuario;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.repository.UsuarioRepository;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.service.AuthService;
import com.veterinaria.service.ControlIntentosLoginService;
import com.veterinaria.util.JwtTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** Mensaje genérico: no revela si falló el correo o la contraseña. */
    static final String CREDENCIALES_INCORRECTAS = "Credenciales incorrectas";

    /**
     * Hash bcrypt de referencia para comparar cuando el correo no existe, de modo que
     * la respuesta tarde lo mismo y no permita descubrir qué correos están registrados.
     * Aunque la contraseña coincida con este hash, el acceso se rechaza porque no hay usuario.
     */
    private static final String HASH_REFERENCIA = "$2y$10$RnNxY7Vyu9xDwJk0wjXKYeht3z7GDYVCsM0HEOWQh.E96j/ryyxiS";

    private final ClienteRepository clienteRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final ControlIntentosLoginService controlIntentos;

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
        if (usuarioRepository.existsByClienteId(cliente.getId())
                || usuarioRepository.existsByCorreoIgnoreCase(cliente.getCorreo())) {
            throw new IllegalArgumentException("El cliente ya tiene una cuenta registrada");
        }

        Usuario usuario = Usuario.builder()
                .nombre(cliente.getNombre())
                .correo(cliente.getCorreo())
                .passwordHash(passwordEncoder.encode(solicitud.getPassword()))
                .cliente(cliente)
                .activo(true)
                .build();
        usuario.getRoles().add(Rol.CLIENTE);
        usuarioRepository.save(usuario);

        return new AuthResponseDTO("Cuenta registrada correctamente", null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO iniciarSesion(LoginAuthDTO solicitud, String ipCliente) {
        String correo = solicitud.getCorreo().trim();
        controlIntentos.verificarPermitido(correo, ipCliente);

        Optional<Usuario> usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .filter(Usuario::estaActivo);
        String hash = usuario.map(Usuario::getPasswordHash).orElse(HASH_REFERENCIA);
        boolean claveValida = passwordEncoder.matches(solicitud.getPassword(), hash);

        if (usuario.isEmpty() || !claveValida || usuario.get().getRoles().isEmpty()) {
            controlIntentos.registrarFallo(correo, ipCliente);
            throw credencialesInvalidas();
        }

        controlIntentos.registrarExito(correo, ipCliente);
        return respuestaConToken("Inicio de sesión exitoso", usuario.get());
    }

    @Override
    @Transactional
    public AuthResponseDTO iniciarSesionVeterinarioDemo(String correoDemo) {
        Veterinario veterinario = veterinarioRepository.findByCorreoIgnoreCase(correoDemo)
                .filter(item -> Boolean.TRUE.equals(item.getActivo()))
                .orElseThrow(() -> new ResourceNotFoundException("El perfil veterinario demo no está disponible"));

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(veterinario.getCorreo())
                .orElseGet(() -> crearCuentaVeterinario(veterinario));
        if (!usuario.estaActivo() || !usuario.tieneRol(Rol.VETERINARIO)) {
            throw new AccessDeniedException("La cuenta del veterinario demo no se encuentra activa");
        }
        return respuestaConToken("Perfil demo iniciado", usuario);
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

    private Usuario crearCuentaVeterinario(Veterinario veterinario) {
        // Cuenta sin contraseña utilizable: solo se accede por el endpoint demo
        // hasta que se le asigne una contraseña real.
        Usuario usuario = Usuario.builder()
                .nombre(veterinario.getNombre())
                .correo(veterinario.getCorreo())
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .veterinario(veterinario)
                .activo(true)
                .build();
        usuario.getRoles().add(Rol.VETERINARIO);
        return usuarioRepository.save(usuario);
    }

    private AuthResponseDTO respuestaConToken(String mensaje, Usuario usuario) {
        String token = jwtTokenService.generarToken(usuario);
        Rol rolPrincipal = usuario.rolPrincipal();
        List<String> roles = usuario.getRoles().stream().sorted().map(Rol::name).toList();
        return new AuthResponseDTO(mensaje, token, "Bearer", jwtTokenService.getExpiracionMs(),
                rolPrincipal == null ? null : rolPrincipal.name(), roles);
    }

    private ClienteNoAutenticadoException credencialesInvalidas() {
        return new ClienteNoAutenticadoException(CREDENCIALES_INCORRECTAS);
    }

    @Override
    @Transactional
    public void cerrarSesion(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(this::credencialesInvalidas);
        usuario.setVersionToken(usuario.getVersionToken() + 1);
        usuarioRepository.save(usuario);
    }
}
