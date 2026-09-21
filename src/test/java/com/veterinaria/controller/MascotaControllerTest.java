package com.veterinaria.controller;

import com.veterinaria.dto.MascotaRegistroDTO;
import com.veterinaria.exception.ClienteNoAutenticadoException;
import com.veterinaria.model.Cliente;
import com.veterinaria.model.Mascota;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.repository.MascotaRepository;
import com.veterinaria.service.impl.MascotaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MascotaControllerTest {

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private MascotaServiceImpl mascotaService;

    @Test
    void registrarMascota_exitosamente() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .nombre("Ana Gómez")
                .activo(true)
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(mascotaRepository.save(any(Mascota.class))).thenAnswer(invocation -> {
            Mascota saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        MascotaRegistroDTO dto = new MascotaRegistroDTO();
        dto.setNombre("Firulais");
        dto.setEspecie("Perro");
        dto.setRaza("Labrador");
        dto.setEdad(3);
        dto.setObservaciones("Muy juguetón");

        Mascota resultado = mascotaService.registrarMascota(dto, 1L);

        assertNotNull(resultado);
        assertEquals("Firulais", resultado.getNombre());
        assertEquals("Perro", resultado.getEspecie());
        assertEquals(cliente, resultado.getCliente());
        verify(mascotaRepository).save(any(Mascota.class));
    }

    @Test
    void registrarMascota_sinObservaciones_debePermitirRegistro() {
        Cliente cliente = Cliente.builder().id(2L).nombre("Luis").activo(true).build();
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
        when(mascotaRepository.save(any(Mascota.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MascotaRegistroDTO dto = new MascotaRegistroDTO();
        dto.setNombre("Milo");
        dto.setEspecie("Gato");
        dto.setRaza("Siamés");
        dto.setEdad(2);
        dto.setObservaciones("");

        Mascota resultado = mascotaService.registrarMascota(dto, 2L);

        assertNotNull(resultado);
        assertNull(resultado.getObservaciones());
    }

    @Test
    void registrarMascota_conNombreVacio_rechazaRegistro() {
        MascotaRegistroDTO dto = new MascotaRegistroDTO();
        dto.setNombre("");
        dto.setEspecie("Perro");
        dto.setRaza("Pastor Alemán");
        dto.setEdad(4);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> mascotaService.registrarMascota(dto, 1L));

        assertEquals("El nombre es obligatorio", exception.getMessage());
    }

    @Test
    void registrarMascota_conEdadNegativa_rechazaRegistro() {
        MascotaRegistroDTO dto = new MascotaRegistroDTO();
        dto.setNombre("Nala");
        dto.setEspecie("Perro");
        dto.setRaza("Cocker");
        dto.setEdad(-1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> mascotaService.registrarMascota(dto, 3L));

        assertEquals("La edad ingresada no es válida", exception.getMessage());
    }

    @Test
    void registrarMascota_sinClienteAutenticado_rechazaRegistro() {
        MascotaRegistroDTO dto = new MascotaRegistroDTO();
        dto.setNombre("Luna");
        dto.setEspecie("Perro");
        dto.setRaza("Pug");
        dto.setEdad(1);

        assertThrows(ClienteNoAutenticadoException.class,
                () -> mascotaService.registrarMascota(dto, null));
    }

    @Test
    void listarMascotasPorCliente_retornaSoloLasDelCliente() {
        Cliente cliente = Cliente.builder().id(4L).nombre("Pedro").activo(true).build();
        when(clienteRepository.findById(4L)).thenReturn(Optional.of(cliente));
        when(mascotaRepository.findByClienteId(4L)).thenReturn(List.of(
                Mascota.builder().id(1L).nombre("Firulais").especie("Perro").raza("Labrador").edad(3).cliente(cliente).build(),
                Mascota.builder().id(2L).nombre("Milo").especie("Gato").raza("Siamés").edad(2).cliente(cliente).build()
        ));

        List<Mascota> mascotas = mascotaService.listarMascotasPorCliente(4L);

        assertEquals(2, mascotas.size());
        assertEquals("Firulais", mascotas.get(0).getNombre());
        assertEquals("Milo", mascotas.get(1).getNombre());
    }
}
