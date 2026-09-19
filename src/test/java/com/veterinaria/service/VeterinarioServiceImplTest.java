package com.veterinaria.service;

import com.veterinaria.dto.VeterinarioRegistroDTO;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.service.impl.VeterinarioServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VeterinarioServiceImplTest {

    @Mock
    private VeterinarioRepository veterinarioRepository;

    @InjectMocks
    private VeterinarioServiceImpl veterinarioService;

    @Test
    void registrarVeterinario_guardaDatosNormalizadosYActivo() {
        VeterinarioRegistroDTO dto = crearDto();
        dto.setNombre("  Laura Gómez  ");
        dto.setDireccion("   ");
        dto.setHorarioAtencion("  Lunes a viernes  ");

        when(veterinarioRepository.save(any(Veterinario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Veterinario resultado = veterinarioService.registrarVeterinario(dto);

        ArgumentCaptor<Veterinario> captor = ArgumentCaptor.forClass(Veterinario.class);
        verify(veterinarioRepository).save(captor.capture());
        assertEquals("Laura Gómez", captor.getValue().getNombre());
        assertEquals("123456789", captor.getValue().getDocumentoIdentidad());
        assertEquals("laura@example.com", captor.getValue().getCorreo());
        assertNull(captor.getValue().getDireccion());
        assertEquals("Lunes a viernes", captor.getValue().getHorarioAtencion());
        assertTrue(resultado.getActivo());
    }

    @Test
    void registrarVeterinario_rechazaDocumentoDuplicado() {
        VeterinarioRegistroDTO dto = crearDto();
        when(veterinarioRepository.existsByDocumentoIdentidad("123456789")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> veterinarioService.registrarVeterinario(dto)
        );

        assertEquals("Este veterinario ya se encuentra registrado", exception.getMessage());
        verify(veterinarioRepository, never()).save(any());
    }

    @Test
    void registrarVeterinario_rechazaCorreoDuplicado() {
        VeterinarioRegistroDTO dto = crearDto();
        when(veterinarioRepository.existsByCorreo("laura@example.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> veterinarioService.registrarVeterinario(dto)
        );

        assertEquals("Este correo ya se encuentra registrado", exception.getMessage());
        verify(veterinarioRepository, never()).save(any());
    }

    private VeterinarioRegistroDTO crearDto() {
        VeterinarioRegistroDTO dto = new VeterinarioRegistroDTO();
        dto.setNombre("Laura Gómez");
        dto.setDocumentoIdentidad("123456789");
        dto.setTelefono("3001234567");
        dto.setCorreo("laura@example.com");
        dto.setDireccion("Calle 10");
        dto.setHorarioAtencion("Lunes a viernes");
        return dto;
    }
}
