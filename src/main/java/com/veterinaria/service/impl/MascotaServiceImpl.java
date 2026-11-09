package com.veterinaria.service.impl;

import com.veterinaria.dto.HistorialCitaDTO;
import com.veterinaria.dto.HistorialMascotaDTO;
import com.veterinaria.dto.MascotaRegistroDTO;
import com.veterinaria.exception.ClienteNoAutenticadoException;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.Cita;
import com.veterinaria.model.Cliente;
import com.veterinaria.model.Mascota;
import com.veterinaria.model.Veterinario;
import com.veterinaria.repository.CitaRepository;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.repository.MascotaRepository;
import com.veterinaria.repository.VeterinarioRepository;
import com.veterinaria.service.MascotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MascotaServiceImpl implements MascotaService {

    private static final String USUARIO_ANONIMO = "anonymousUser";
    private static final List<String> ESTADOS_HISTORIAL = List.of(
            CitaServiceImpl.ESTADO_COMPLETADA,
            CitaServiceImpl.ESTADO_NO_ASISTIO,
            CitaServiceImpl.ESTADO_ANULADA,
            CitaServiceImpl.ESTADO_CANCELADA_PROFESIONAL);

    private final MascotaRepository mascotaRepository;
    private final ClienteRepository clienteRepository;
    private final CitaRepository citaRepository;
    private final VeterinarioRepository veterinarioRepository;

    @Override
    public Mascota registrarMascota(MascotaRegistroDTO dto, Long clienteId) {
        validarClienteAutenticado(clienteId);
        validarDatosMascota(dto);

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + clienteId));

        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new IllegalArgumentException("El cliente no se encuentra activo");
        }

        String sexoNormalizado = normalizarSexo(dto.getSexo());

        Mascota mascota = Mascota.builder()
                .nombre(dto.getNombre().trim())
                .especie(dto.getEspecie().trim())
                .raza(dto.getRaza().trim())
                .edad(dto.getEdad())
                .sexo(sexoNormalizado)
                .observaciones(dto.getObservaciones() == null || dto.getObservaciones().isBlank() ? null : dto.getObservaciones().trim())
                .cliente(cliente)
                .build();

        return mascotaRepository.save(mascota);
    }

    @Override
    public List<Mascota> listarMascotasPorCliente(Long clienteId) {
        validarClienteAutenticado(clienteId);
        clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + clienteId));
        return mascotaRepository.findByClienteId(clienteId);
    }

    @Override
    @Transactional(readOnly = true)
    public HistorialMascotaDTO consultarHistorial(Long mascotaId, String correoAutenticado, int page, int limit) {
        if (correoAutenticado == null || correoAutenticado.isBlank() || USUARIO_ANONIMO.equals(correoAutenticado)) {
            throw new ClienteNoAutenticadoException(
                    "Se requiere un usuario autenticado para consultar el historial de la mascota");
        }
        if (page < 0) {
            throw new IllegalArgumentException("El número de página no puede ser negativo");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("El límite de resultados debe estar entre 1 y 100");
        }

        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con id: " + mascotaId));

        autorizarConsultaHistorial(mascota, correoAutenticado);

        Pageable pageable = PageRequest.of(page, limit,
                Sort.by(Sort.Order.desc("fechaHoraInicio"), Sort.Order.desc("id")));
        Page<Cita> pagina = citaRepository.findByMascotaIdAndEstadoIn(mascotaId, ESTADOS_HISTORIAL, pageable);

        List<HistorialCitaDTO> contenido = new ArrayList<>();
        for (Cita cita : pagina.getContent()) {
            contenido.add(toHistorialCitaDTO(cita));
        }

        return new HistorialMascotaDTO(
                mascota.getId(),
                mascota.getNombre(),
                contenido,
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }

    private void autorizarConsultaHistorial(Mascota mascota, String correoAutenticado) {
        Optional<Cliente> cliente = clienteRepository.findByCorreoIgnoreCase(correoAutenticado)
                .filter(item -> Boolean.TRUE.equals(item.getActivo()));
        if (cliente.isPresent() && mascota.getCliente().getId().equals(cliente.get().getId())) {
            return;
        }

        Optional<Veterinario> veterinario = veterinarioRepository.findByCorreoIgnoreCase(correoAutenticado)
                .filter(item -> Boolean.TRUE.equals(item.getActivo()));
        if (veterinario.isPresent()
                && citaRepository.existsByMascotaIdAndVeterinarioId(mascota.getId(), veterinario.get().getId())) {
            return;
        }

        throw new AccessDeniedException("No tienes permisos para consultar el historial de esta mascota");
    }

    private HistorialCitaDTO toHistorialCitaDTO(Cita cita) {
        Veterinario veterinario = cita.getVeterinario();
        Veterinario canceladaPorVeterinario = cita.getCanceladaPorVeterinario();
        return new HistorialCitaDTO(
                cita.getId(),
                cita.getEstado(),
                CitaServiceImpl.etiquetaEstado(cita.getEstado()),
                cita.getFechaHoraInicio(),
                cita.getFechaHoraFin(),
                veterinario.getNombre(),
                veterinario.getEspecialidad(),
                cita.getServicio().getNombre(),
                cita.getServicio().getCategoria(),
                cita.getDiagnostico(),
                cita.getTratamiento(),
                cita.getNotasClinicas(),
                cita.getMotivoCancelacion(),
                cita.getCanceladaAt(),
                canceladaPorVeterinario == null ? null : canceladaPorVeterinario.getNombre());
    }

    private void validarClienteAutenticado(Long clienteId) {
        if (clienteId == null) {
            throw new ClienteNoAutenticadoException("Debe enviar el header X-Cliente-Id o el parámetro clienteId para identificar al cliente");
        }
    }

    private void validarDatosMascota(MascotaRegistroDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la mascota son obligatorios");
        }
        if (dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (dto.getEspecie() == null || dto.getEspecie().isBlank()) {
            throw new IllegalArgumentException("La especie es obligatoria");
        }
        if (dto.getRaza() == null || dto.getRaza().isBlank()) {
            throw new IllegalArgumentException("La raza es obligatoria");
        }
        if (dto.getEdad() == null || dto.getEdad() < 0) {
            throw new IllegalArgumentException("La edad ingresada no es válida");
        }
        if (dto.getSexo() == null || dto.getSexo().isBlank()) {
            throw new IllegalArgumentException("El sexo es obligatorio");
        }
        String sexo = dto.getSexo().trim();
        if (!"Hembra".equalsIgnoreCase(sexo) && !"Macho".equalsIgnoreCase(sexo)) {
            throw new IllegalArgumentException("El sexo debe ser Hembra o Macho");
        }
        if (dto.getObservaciones() != null && dto.getObservaciones().length() > 500) {
            throw new IllegalArgumentException("Las observaciones no pueden superar los 500 caracteres");
        }
    }

    private String normalizarSexo(String sexo) {
        if (sexo == null || sexo.isBlank()) {
            throw new IllegalArgumentException("El sexo es obligatorio");
        }
        String sexoNormalizado = sexo.trim();
        if (!"Hembra".equalsIgnoreCase(sexoNormalizado) && !"Macho".equalsIgnoreCase(sexoNormalizado)) {
            throw new IllegalArgumentException("El sexo debe ser Hembra o Macho");
        }
        return sexoNormalizado.substring(0, 1).toUpperCase() + sexoNormalizado.substring(1).toLowerCase();
    }
}
