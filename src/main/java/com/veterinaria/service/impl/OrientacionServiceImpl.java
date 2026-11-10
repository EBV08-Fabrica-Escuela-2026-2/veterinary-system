package com.veterinaria.service.impl;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.dto.OrientacionRequestDTO;
import com.veterinaria.dto.OrientacionResponseDTO;
import com.veterinaria.dto.ServicioCatalogoDTO;
import com.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.model.Cliente;
import com.veterinaria.model.Mascota;
import com.veterinaria.repository.ClienteRepository;
import com.veterinaria.repository.MascotaRepository;
import com.veterinaria.service.CatalogoService;
import com.veterinaria.service.OrientacionAiClient;
import com.veterinaria.service.OrientacionAiException;
import com.veterinaria.service.OrientacionAiClient.Resultado;
import com.veterinaria.service.OrientacionAiClient.ServicioCandidato;
import com.veterinaria.service.OrientacionAiClient.ServicioSeleccionado;
import com.veterinaria.service.OrientacionService;

@Service
public class OrientacionServiceImpl implements OrientacionService {

    private static final Logger logger = LoggerFactory.getLogger(OrientacionServiceImpl.class);
    private static final Pattern PALABRAS = Pattern.compile("[\\p{L}]{4,}");
    private static final int MAX_RECOMMENDATIONS = 3;
    private static final String ADVERTENCIA = "Esta orientación no es un diagnóstico ni reemplaza la valoración de un veterinario.";
    private static final String MENSAJE_URGENCIA = "Si observas signos graves o un empeoramiento rápido, busca atención veterinaria inmediata.";
    private static final Set<String> PALABRAS_COMUNES = Set.of(
            "para", "como", "desde", "hace", "esta", "este", "tiene", "tengo", "sido", "mucho",
            "poco", "mascota", "mascotas", "perro", "perra", "gato", "gata", "sintoma", "sintomas",
            "preocupacion", "alto", "moderado", "bajo", "horas", "dias", "semanas", "anos",
            "presenta", "presentan", "observa", "observan", "animal", "animales");
    private static final List<String> SIGNOS_ALARMA = List.of(
            "no puede respirar", "dificultad para respirar", "dejo de respirar",
            "convulsion", "inconsciente", "no responde", "sangrado abundante",
            "sangra mucho", "intoxicacion", "ingirio veneno", "golpe de calor",
            "abdomen muy hinchado", "encías palidas", "encias palidas");

    private final ClienteRepository clienteRepository;
    private final MascotaRepository mascotaRepository;
    private final CatalogoService catalogoService;
    private final OrientacionAiClient aiClient;

    public OrientacionServiceImpl(
            ClienteRepository clienteRepository,
            MascotaRepository mascotaRepository,
            CatalogoService catalogoService,
            OrientacionAiClient aiClient) {
        this.clienteRepository = clienteRepository;
        this.mascotaRepository = mascotaRepository;
        this.catalogoService = catalogoService;
        this.aiClient = aiClient;
    }

    @Override
    @Transactional(readOnly = true)
    public OrientacionResponseDTO orientar(String correoCliente, OrientacionRequestDTO solicitud) {
        Cliente cliente = clienteRepository.findByCorreoIgnoreCase(correoCliente)
                .filter(Cliente::getActivo)
                .orElseThrow(() -> new AccessDeniedException("No se encontró un cliente activo para esta sesión"));
        Mascota mascota = mascotaRepository.findByIdAndClienteId(solicitud.mascotaId(), cliente.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada para este cliente"));

        Map<Long, ServicioDisponible> servicios = construirCatalogo();
        Set<Long> coincidencias = new HashSet<>(servicios.keySet());
        List<ServicioCandidato> candidatos = servicios.values().stream()
                .map(item -> new ServicioCandidato(
                        item.servicio().getId(),
                        item.servicio().getNombre(),
                        item.servicio().getDescripcion() == null ? "" : item.servicio().getDescripcion()))
                .toList();

        boolean urgente = contieneSignoAlarma(solicitud.sintomas());
        boolean modoRespaldo = false;
        List<ServicioSeleccionado> seleccionados;
        try {
            Resultado resultado = aiClient.analizar(
                    mascota.getEspecie(),
                    mascota.getEdad(),
                    solicitud.sintomas().trim(),
                    solicitud.duracionValor(),
                    solicitud.duracionUnidad().name(),
                    solicitud.nivelPreocupacion().name(),
                    candidatos);
            urgente = urgente || resultado.urgente();
            seleccionados = filtrarSeleccionados(resultado.recomendaciones(), coincidencias);
            if (seleccionados.isEmpty() && !resultado.recomendaciones().isEmpty()) {
                modoRespaldo = true;
                seleccionados = buscarCoincidenciasCatalogo(solicitud.sintomas(), servicios);
            }
        } catch (OrientacionAiException ex) {
            logger.warn("Orientación Gemini no disponible; se aplicará respaldo de catálogo ({})",
                    ex.getClass().getSimpleName());
            modoRespaldo = true;
            seleccionados = buscarCoincidenciasCatalogo(solicitud.sintomas(), servicios);
        }

        List<OrientacionResponseDTO.ServicioRecomendadoDTO> recomendaciones = seleccionados.stream()
                .map(seleccionado -> {
                    ServicioDisponible disponible = servicios.get(seleccionado.servicioId());
                    return new OrientacionResponseDTO.ServicioRecomendadoDTO(
                            disponible.servicio().getId(),
                            disponible.servicio().getNombre(),
                            seleccionado.explicacion(),
                            disponible.veterinarios());
                })
                .toList();

        String mensaje = recomendaciones.isEmpty()
                ? "No encontramos una relación clara entre los síntomas y los servicios activos del catálogo. Consulta directamente con un veterinario."
                : modoRespaldo
                        ? "No pudimos completar el análisis de IA. Estas opciones se encontraron por coincidencias claras en el catálogo; confirma con un veterinario cuál es adecuada."
                        : "Las opciones sugeridas se relacionan con lo descrito según la IA y corresponden a servicios activos del catálogo.";

        return new OrientacionResponseDTO(
                urgente,
                urgente ? MENSAJE_URGENCIA : null,
                modoRespaldo,
                mensaje,
                recomendaciones,
                ADVERTENCIA);
    }

    private Map<Long, ServicioDisponible> construirCatalogo() {
        Map<Long, ServicioDisponible> servicios = new HashMap<>();
        catalogoService.consultarCatalogo().getVeterinarios().forEach(veterinario -> {
            if (veterinario.getId() == null || veterinario.getNombre() == null) {
                return;
            }
            List<ServicioCatalogoDTO> serviciosActivos = veterinario.getServicios() == null
                    ? List.of()
                    : veterinario.getServicios();
            serviciosActivos.forEach(servicio -> {
                if (servicio.getId() == null || servicio.getNombre() == null) {
                    return;
                }
                ServicioDisponible disponible = servicios.computeIfAbsent(
                        servicio.getId(), ignored -> new ServicioDisponible(servicio, new ArrayList<>()));
                if (disponible.veterinarios().stream().noneMatch(vet -> vet.id().equals(veterinario.getId()))) {
                    disponible.veterinarios().add(
                            new OrientacionResponseDTO.VeterinarioSugeridoDTO(veterinario.getId(), veterinario.getNombre()));
                }
            });
        });
        return servicios;
    }

    private List<ServicioSeleccionado> filtrarSeleccionados(
            List<ServicioSeleccionado> seleccionados,
            Set<Long> idsPermitidos) {
        if (seleccionados == null) {
            return List.of();
        }
        return seleccionados.stream()
                .filter(item -> item != null
                        && idsPermitidos.contains(item.servicioId())
                        && item.explicacion() != null
                        && !item.explicacion().isBlank())
                .distinct()
                .limit(MAX_RECOMMENDATIONS)
                .toList();
    }

    private List<ServicioSeleccionado> buscarCoincidenciasCatalogo(
            String sintomas,
            Map<Long, ServicioDisponible> servicios) {
        Set<String> terminosSintomas = extraerTerminos(sintomas);
        if (terminosSintomas.isEmpty()) {
            return List.of();
        }

        return servicios.values().stream()
                .map(disponible -> {
                    String textoServicio = disponible.servicio().getNombre() + " "
                            + (disponible.servicio().getDescripcion() == null ? "" : disponible.servicio().getDescripcion());
                    Set<String> terminosServicio = extraerTerminos(textoServicio);
                    long coincidencias = terminosSintomas.stream().filter(terminosServicio::contains).count();
                    return Map.entry(disponible.servicio(), coincidencias);
                })
                .filter(item -> item.getValue() > 0)
                .sorted(Map.Entry.<ServicioCatalogoDTO, Long>comparingByValue().reversed())
                .limit(MAX_RECOMMENDATIONS)
                .map(item -> new ServicioSeleccionado(
                        item.getKey().getId(),
                        "El nombre o la descripción del servicio coincide con términos de los síntomas; esta coincidencia no confirma pertinencia clínica."))
                .toList();
    }

    private Set<String> extraerTerminos(String texto) {
        String normalizado = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        var matcher = PALABRAS.matcher(normalizado);
        Set<String> terminos = new HashSet<>();
        while (matcher.find()) {
            String termino = matcher.group();
            if (!PALABRAS_COMUNES.contains(termino)) {
                terminos.add(termino);
            }
        }
        return terminos;
    }

    private boolean contieneSignoAlarma(String sintomas) {
        String normalizado = Normalizer.normalize(sintomas.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return SIGNOS_ALARMA.stream().anyMatch(signo -> {
            String signoNormalizado = Normalizer.normalize(signo, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "");
            return normalizado.contains(signoNormalizado);
        });
    }

    private record ServicioDisponible(
            ServicioCatalogoDTO servicio,
            List<OrientacionResponseDTO.VeterinarioSugeridoDTO> veterinarios) {
    }
}
