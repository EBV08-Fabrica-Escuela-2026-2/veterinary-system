package com.veterinaria.service;

import com.veterinaria.dto.AuthResponseDTO;
import com.veterinaria.dto.LoginAuthDTO;
import com.veterinaria.dto.RegistroAuthDTO;
import com.veterinaria.exception.ClienteNoAutenticadoException;
import com.veterinaria.model.Cliente;
import com.veterinaria.model.ClienteCredencial;
import com.veterinaria.repository.ClienteCredencialRepository;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.service.impl.AuthServiceImpl;
import com.veterinaria.util.JwtTokenService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteCredencialRepository credencialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registrar_guardaHashParaClienteExistente() {
        Cliente cliente = Cliente.builder().id(1L).correo("alex@gmail.com").activo(true).build();
        when(clienteRepository.findByCorreoIgnoreCaseAndDocumentoIdentidad("alex@gmail.com", "12345678"))
                .thenReturn(Optional.of(cliente));
        when(credencialRepository.existsByClienteId(1L)).thenReturn(false);
        when(passwordEncoder.encode("ClaveSegura123")).thenReturn("bcrypt-hash");

        AuthResponseDTO response = authService.registrar(registro());

        ArgumentCaptor<ClienteCredencial> captor = ArgumentCaptor.forClass(ClienteCredencial.class);
        verify(credencialRepository).save(captor.capture());
        assertEquals(cliente, captor.getValue().getCliente());
        assertEquals("bcrypt-hash", captor.getValue().getPasswordHash());
        assertEquals("Cuenta registrada correctamente", response.getMensaje());
        verify(passwordEncoder).encode("ClaveSegura123");
    }

    @Test
    void iniciarSesion_devuelveTokenBearerCuandoLaClaveEsCorrecta() {
        Cliente cliente = Cliente.builder().id(1L).correo("alex@gmail.com").activo(true).build();
        ClienteCredencial credencial = ClienteCredencial.builder().cliente(cliente)
                .passwordHash("bcrypt-hash").build();
        when(clienteRepository.findByCorreoIgnoreCase("alex@gmail.com")).thenReturn(Optional.of(cliente));
        when(credencialRepository.findByClienteId(1L)).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("ClaveSegura123", "bcrypt-hash")).thenReturn(true);
        when(jwtTokenService.generarToken("alex@gmail.com")).thenReturn("jwt-real");
        when(jwtTokenService.getExpiracionMs()).thenReturn(86_400_000L);

        AuthResponseDTO response = authService.iniciarSesion(login());

        assertEquals("jwt-real", response.getToken());
        assertEquals("Bearer", response.getTipoToken());
        assertEquals(86_400_000L, response.getExpiraEnMs());
    }

    @Test
    void iniciarSesion_rechazaContraseñaIncorrectaSinEmitirToken() {
        Cliente cliente = Cliente.builder().id(1L).correo("alex@gmail.com").activo(true).build();
        ClienteCredencial credencial = ClienteCredencial.builder().cliente(cliente)
                .passwordHash("bcrypt-hash").build();
        when(clienteRepository.findByCorreoIgnoreCase("alex@gmail.com")).thenReturn(Optional.of(cliente));
        when(credencialRepository.findByClienteId(1L)).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("ClaveSegura123", "bcrypt-hash")).thenReturn(false);

        assertThrows(ClienteNoAutenticadoException.class, () -> authService.iniciarSesion(login()));

        verify(jwtTokenService, never()).generarToken("alex@gmail.com");
    }

    private RegistroAuthDTO registro() {
        RegistroAuthDTO dto = new RegistroAuthDTO();
        dto.setCorreo("alex@gmail.com");
        dto.setDocumentoIdentidad("12345678");
        dto.setPassword("ClaveSegura123");
        return dto;
    }

    private LoginAuthDTO login() {
        LoginAuthDTO dto = new LoginAuthDTO();
        dto.setCorreo("alex@gmail.com");
        dto.setPassword("ClaveSegura123");
        return dto;
    }
}