package com.veterinaria.service;

import com.veterinaria.dto.AuthResponseDTO;
import com.veterinaria.dto.ClienteResponseDTO;
import com.veterinaria.dto.LoginAuthDTO;
import com.veterinaria.dto.RegistroAuthDTO;
import com.veterinaria.exception.ClienteNoAutenticadoException;
import com.veterinaria.exception.DemasiadosIntentosException;
import com.veterinaria.model.Cliente;
import com.veterinaria.model.Rol;
import com.veterinaria.model.Usuario;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.repository.UsuarioRepository;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.service.impl.AuthServiceImpl;
import com.veterinaria.util.JwtTokenService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String IP = "203.0.113.10";

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private VeterinarioRepository veterinarioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private ControlIntentosLoginService controlIntentos;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registrar_creaUnUsuarioClienteConLaContraseñaCifrada() {
        Cliente cliente = Cliente.builder().id(1L).nombre("Alex").correo("alex@gmail.com").activo(true).build();
        when(clienteRepository.findByCorreoIgnoreCaseAndDocumentoIdentidad("alex@gmail.com", "12345678"))
                .thenReturn(Optional.of(cliente));
        when(usuarioRepository.existsByClienteId(1L)).thenReturn(false);
        when(usuarioRepository.existsByCorreoIgnoreCase("alex@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("ClaveSegura123")).thenReturn("bcrypt-hash");

        AuthResponseDTO response = authService.registrar(registro());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertEquals(cliente, guardado.getCliente());
        assertEquals("bcrypt-hash", guardado.getPasswordHash());
        assertEquals(Set.of(Rol.CLIENTE), guardado.getRoles());
        assertEquals("Cuenta registrada correctamente", response.getMensaje());
    }

    @Test
    void registrar_rechazaUnClienteQueYaTieneCuenta() {
        Cliente cliente = Cliente.builder().id(1L).correo("alex@gmail.com").activo(true).build();
        when(clienteRepository.findByCorreoIgnoreCaseAndDocumentoIdentidad("alex@gmail.com", "12345678"))
                .thenReturn(Optional.of(cliente));
        when(usuarioRepository.existsByClienteId(1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.registrar(registro()));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void iniciarSesion_devuelveTokenBearerConElRolDelUsuario() {
        Usuario usuario = usuario("laura@vet.com", Rol.VETERINARIO);
        when(usuarioRepository.findByCorreoIgnoreCase("laura@vet.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("ClaveSegura123", "bcrypt-hash")).thenReturn(true);
        when(jwtTokenService.generarToken(usuario)).thenReturn("jwt-real");
        when(jwtTokenService.getExpiracionMs()).thenReturn(86_400_000L);

        AuthResponseDTO response = authService.iniciarSesion(login("laura@vet.com"), IP);

        assertEquals("jwt-real", response.getToken());
        assertEquals("Bearer", response.getTipoToken());
        assertEquals(86_400_000L, response.getExpiraEnMs());
        assertEquals("VETERINARIO", response.getRol());
        assertEquals(List.of("VETERINARIO"), response.getRoles());
        verify(controlIntentos).verificarPermitido("laura@vet.com", IP);
        verify(controlIntentos).registrarExito("laura@vet.com", IP);
    }

    @Test
    void iniciarSesion_priorizaElRolAdministradorComoRolPrincipal() {
        Usuario usuario = usuario("ana@vet.com", Rol.VETERINARIO, Rol.ADMINISTRADOR);
        when(usuarioRepository.findByCorreoIgnoreCase("ana@vet.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("ClaveSegura123", "bcrypt-hash")).thenReturn(true);
        when(jwtTokenService.generarToken(usuario)).thenReturn("jwt");

        AuthResponseDTO response = authService.iniciarSesion(login("ana@vet.com"), IP);

        assertEquals("ADMINISTRADOR", response.getRol());
        assertEquals(List.of("ADMINISTRADOR", "VETERINARIO"), response.getRoles());
    }

    @Test
    void iniciarSesion_rechazaContraseñaIncorrectaConMensajeGenericoYRegistraElFallo() {
        Usuario usuario = usuario("alex@gmail.com", Rol.CLIENTE);
        when(usuarioRepository.findByCorreoIgnoreCase("alex@gmail.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("ClaveSegura123", "bcrypt-hash")).thenReturn(false);

        ClienteNoAutenticadoException error = assertThrows(ClienteNoAutenticadoException.class,
                () -> authService.iniciarSesion(login("alex@gmail.com"), IP));

        assertEquals("Credenciales incorrectas", error.getMessage());
        verify(controlIntentos).registrarFallo("alex@gmail.com", IP);
        verify(jwtTokenService, never()).generarToken(any());
    }

    @Test
    void iniciarSesion_correoNoRegistradoDevuelveElMismoMensajeQueUnaClaveErronea() {
        when(usuarioRepository.findByCorreoIgnoreCase("nadie@gmail.com")).thenReturn(Optional.empty());

        ClienteNoAutenticadoException error = assertThrows(ClienteNoAutenticadoException.class,
                () -> authService.iniciarSesion(login("nadie@gmail.com"), IP));

        assertEquals("Credenciales incorrectas", error.getMessage());
        // Compara contra un hash de referencia para no delatar por tiempo de respuesta que el correo no existe.
        verify(passwordEncoder).matches(anyString(), anyString());
        verify(controlIntentos).registrarFallo("nadie@gmail.com", IP);
    }

    @Test
    void iniciarSesion_rechazaUnaCuentaInactivaAunqueLaClaveSeaCorrecta() {
        Usuario usuario = usuario("alex@gmail.com", Rol.CLIENTE);
        usuario.setActivo(false);
        when(usuarioRepository.findByCorreoIgnoreCase("alex@gmail.com")).thenReturn(Optional.of(usuario));

        assertThrows(ClienteNoAutenticadoException.class,
                () -> authService.iniciarSesion(login("alex@gmail.com"), IP));
        verify(jwtTokenService, never()).generarToken(any());
    }

    @Test
    void iniciarSesion_bloqueadoNoValidaLaContraseñaNiEmiteToken() {
        doThrow(new DemasiadosIntentosException(900)).when(controlIntentos)
                .verificarPermitido("alex@gmail.com", IP);

        DemasiadosIntentosException error = assertThrows(DemasiadosIntentosException.class,
                () -> authService.iniciarSesion(login("alex@gmail.com"), IP));

        assertEquals(900, error.getSegundosRestantes());
        assertEquals("Demasiados intentos fallidos. Intenta nuevamente en 15 minutos.", error.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenService, never()).generarToken(any());
    }

    @Test
    void obtenerPerfil_devuelveElClienteAutenticado() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .nombre("Alex Gómez")
                .documentoIdentidad("12345678")
                .telefono("3001234567")
                .correo("alex@gmail.com")
                .direccion("Calle 10")
                .activo(true)
                .build();
        when(clienteRepository.findByCorreoIgnoreCase("alex@gmail.com")).thenReturn(Optional.of(cliente));

        ClienteResponseDTO response = authService.obtenerPerfil("alex@gmail.com");

        assertEquals(1L, response.getId());
        assertEquals("Alex Gómez", response.getNombre());
        assertEquals("12345678", response.getDocumentoIdentidad());
        assertEquals("alex@gmail.com", response.getCorreo());
    }

    private Usuario usuario(String correo, Rol... roles) {
        return Usuario.builder()
                .id(10L)
                .nombre("Usuario de prueba")
                .correo(correo)
                .passwordHash("bcrypt-hash")
                .activo(true)
                .roles(new HashSet<>(List.of(roles)))
                .build();
    }

    private RegistroAuthDTO registro() {
        RegistroAuthDTO dto = new RegistroAuthDTO();
        dto.setCorreo("alex@gmail.com");
        dto.setDocumentoIdentidad("12345678");
        dto.setPassword("ClaveSegura123");
        return dto;
    }

    private LoginAuthDTO login(String correo) {
        LoginAuthDTO dto = new LoginAuthDTO();
        dto.setCorreo(correo);
        dto.setPassword("ClaveSegura123");
        return dto;
    }
}
