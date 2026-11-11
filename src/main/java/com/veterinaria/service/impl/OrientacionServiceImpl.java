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
    private static final List<String> SINTOMAS_PIEL = List.of(
            "picazon", "picason", "pizacon", "pizazon", "comezon", "prurito", "roncha", "sarpullido",
            "rasca", "rascado",
            "irritacion en la piel", "problema de piel", "alergia en la piel");
    private static final List<String> SERVICIOS_PIEL = List.of(
            "dermatolog", "problema de piel", "problemas de piel", "tratamiento de piel");
    private static final List<String> SINTOMAS_TRAUMA = List.of(
            "fractura", "fracturado", "hueso roto", "hueso quebrado", "quebradura", "traumatismo",
            "atropellamiento", "atropellado", "golpe fuerte", "caida fuerte");
    private static final List<String> SERVICIOS_TRAUMA = List.of(
            "cirugia", "quirurg", "ortoped", "traumatolog", "fractura");
    private static final List<String> SERVICIOS_NO_TRAUMA = List.of(
            "cardiac", "corazon", "oftalm");
    private static final List<String> SINTOMAS_DIGESTIVOS = List.of(
            "dolor abdominal", "dolro abdominal", "dolor abdomial", "dolor de estomago",
            "dolor en la barriga", "vomito", "vomitos", "vomitando", "nausea", "nauseas",
            "arcadas", "diarrea", "abdomen doloroso");
    private static final List<String> SERVICIOS_DIGESTIVOS = List.of(
            "ecografia", "ultrasonido", "gastroenterolog", "medicina interna", "digestiv");
    private static final List<String> SINTOMAS_RESPIRATORIOS = List.of(
            "tos", "estornudo", "estornudos", "respira rapido", "respiracion agitada", "silbido al respirar",
            "secrecion nasal", "moqueo", "congestion", "dificultad para respirar");
    private static final List<String> SERVICIOS_RESPIRATORIOS = List.of(
            "respirator", "neumolog", "medicina interna");
    private static final List<String> SINTOMAS_URINARIOS = List.of(
            "no puede orinar", "dificultad para orinar", "orina con sangre", "sangre en la orina",
            "orina muchas veces", "orina poco", "no orina", "problema urinario", "problemas urinarios");
    private static final List<String> SERVICIOS_URINARIOS = List.of(
            "urolog", "nefrolog", "urinari", "medicina interna");
    private static final List<String> SINTOMAS_OCULARES = List.of(
            "ojo rojo", "ojos rojos", "lagrimeo", "secrecion en el ojo", "ojo cerrado",
            "hinchazon del ojo", "problema ocular", "problema en los ojos");
    private static final List<String> SERVICIOS_OCULARES = List.of(
            "oftalm", "ocular", "ojos");
    private static final List<String> SINTOMAS_OIDOS = List.of(
            "dolor de oido", "dolor en el oido", "orejas rojas", "sacude la cabeza",
            "se rasca las orejas", "secrecion en el oido", "problema de oido");
    private static final List<String> SERVICIOS_OIDOS = List.of(
            "otorrino", "oido", "auditiv", "dermatolog");
    private static final List<String> SINTOMAS_DENTALES = List.of(
            "dolor dental", "dolor de dientes", "mal aliento", "diente roto",
            "dientes sucios", "problema dental", "no puede masticar");
    private static final List<String> SERVICIOS_DENTALES = List.of(
            "odontolog", "dental", "dent");
    private static final List<String> SINTOMAS_MOVILIDAD = List.of(
            "cojea", "cojera", "no apoya la pata", "no puede caminar", "dificultad para caminar",
            "dolor en la pata", "pata hinchada", "dolor articular", "dolor en las articulaciones");
    private static final List<String> SERVICIOS_MOVILIDAD = List.of(
            "ortoped", "traumatolog", "rehabilit", "fisioterap", "cirugia", "quirurg");
    private static final List<String> SERVICIOS_CONSULTA_GENERAL = List.of(
            "consulta general", "medicina general", "consulta veterinaria", "valoracion general",
            "control veterinario", "revision completa del paciente");
    private static final List<String> SIGNOS_ALARMA = List.of(
            "no puede respirar", "dificultad para respirar", "dejo de respirar",
            "convulsion", "inconsciente", "no responde", "sangrado abundante",
            "sangra mucho", "intoxicacion", "ingirio veneno", "golpe de calor",
            "abdomen muy hinchado", "abdomen hinchado", "vomito con sangre",
            "vomita sangre", "vomitos con sangre", "encias palidas");

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
                .sorted((primero, segundo) -> Long.compare(
                        primero.servicio().getId(), segundo.servicio().getId()))
                .map(item -> new ServicioCandidato(
                        item.servicio().getId(),
                        item.servicio().getNombre(),
                        item.servicio().getDescripcion() == null ? "" : item.servicio().getDescripcion(),
                        item.servicio().getCategoria() == null ? "" : item.servicio().getCategoria()))
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
                    candidatos);
            urgente = urgente || resultado.urgente();
            seleccionados = filtrarSeleccionados(resultado.recomendaciones(), coincidencias);
            if (seleccionados.isEmpty()) {
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
        String sintomasNormalizados = normalizar(sintomas);
        Map<Long, CoincidenciaCatalogo> coincidencias = new HashMap<>();

        if (coincideConAlguno(sintomasNormalizados, SINTOMAS_PIEL)) {
            agregarCoincidenciasEspecialidad(
                    servicios, coincidencias, SERVICIOS_PIEL, List.of(),
                    "La picazón o las lesiones de piel descritas pueden ser valoradas en este servicio.");
        }
        if (coincideConAlguno(sintomasNormalizados, SINTOMAS_TRAUMA)) {
            agregarCoincidenciasEspecialidad(
                    servicios, coincidencias, SERVICIOS_TRAUMA, SERVICIOS_NO_TRAUMA,
                    "Una posible fractura o lesión por trauma requiere valoración veterinaria; este servicio ofrece atención quirúrgica o relacionada.");
        }
        if (coincideConAlguno(sintomasNormalizados, SINTOMAS_DIGESTIVOS)) {
            agregarCoincidenciasEspecialidad(
                    servicios, coincidencias, SERVICIOS_DIGESTIVOS, List.of(),
                    "El dolor abdominal y los vómitos pueden requerir valoración veterinaria y, si está disponible, un examen de imagen.");
            agregarConsultaGeneral(
                    servicios, coincidencias, 5,
                    "Una consulta general permite valorar el dolor abdominal y los vómitos y definir los siguientes pasos.");
        }
        if (coincideConAlguno(sintomasNormalizados, SINTOMAS_RESPIRATORIOS)) {
            agregarCoincidenciasEspecialidad(
                    servicios, coincidencias, SERVICIOS_RESPIRATORIOS, List.of(),
                    "Los síntomas respiratorios descritos requieren valoración veterinaria; este servicio puede orientar la evaluación.");
        }
        if (coincideConAlguno(sintomasNormalizados, SINTOMAS_URINARIOS)) {
            agregarCoincidenciasEspecialidad(
                    servicios, coincidencias, SERVICIOS_URINARIOS, List.of(),
                    "Los cambios al orinar requieren valoración veterinaria; este servicio puede orientar la evaluación.");
        }
        if (coincideConAlguno(sintomasNormalizados, SINTOMAS_OCULARES)) {
            agregarCoincidenciasEspecialidad(
                    servicios, coincidencias, SERVICIOS_OCULARES, List.of(),
                    "Los síntomas en los ojos pueden valorarse en este servicio.");
        }
        if (coincideConAlguno(sintomasNormalizados, SINTOMAS_OIDOS)) {
            agregarCoincidenciasEspecialidad(
                    servicios, coincidencias, SERVICIOS_OIDOS, List.of(),
                    "La molestia descrita en los oídos puede valorarse en este servicio.");
        }
        if (coincideConAlguno(sintomasNormalizados, SINTOMAS_DENTALES)) {
            agregarCoincidenciasEspecialidad(
                    servicios, coincidencias, SERVICIOS_DENTALES, List.of(),
                    "La molestia descrita en los dientes puede valorarse en este servicio.");
        }
        if (coincideConAlguno(sintomasNormalizados, SINTOMAS_MOVILIDAD)) {
            agregarCoincidenciasEspecialidad(
                    servicios, coincidencias, SERVICIOS_MOVILIDAD, SERVICIOS_NO_TRAUMA,
                    "La dificultad para caminar o el dolor en una pata pueden requerir valoración musculoesquelética.");
        }

        if (coincidencias.isEmpty()) {
            agregarCoincidenciasTextuales(sintomas, servicios, coincidencias);
        }
        if (coincidencias.isEmpty()) {
            agregarConsultaGeneral(
                    servicios, coincidencias, 1,
                    "Una consulta general permite que un veterinario valore los síntomas y determine la atención adecuada.");
        }

        return coincidencias.entrySet().stream()
                .sorted((primera, segunda) -> {
                    int porPertinencia = Integer.compare(
                            segunda.getValue().puntaje(), primera.getValue().puntaje());
                    return porPertinencia != 0
                            ? porPertinencia
                            : Long.compare(primera.getKey(), segunda.getKey());
                })
                .limit(MAX_RECOMMENDATIONS)
                .map(item -> new ServicioSeleccionado(item.getKey(), item.getValue().explicacion()))
                .toList();
    }

    private void agregarConsultaGeneral(
            Map<Long, ServicioDisponible> servicios,
            Map<Long, CoincidenciaCatalogo> coincidencias,
            int puntaje,
            String explicacion) {
        servicios.values().stream()
                .filter(disponible -> coincideConAlguno(
                        textoServicio(disponible), SERVICIOS_CONSULTA_GENERAL))
                .forEach(disponible -> coincidencias.putIfAbsent(
                        disponible.servicio().getId(), new CoincidenciaCatalogo(puntaje, explicacion)));
    }

    private void agregarCoincidenciasEspecialidad(
            Map<Long, ServicioDisponible> servicios,
            Map<Long, CoincidenciaCatalogo> coincidencias,
            List<String> terminosServicio,
            List<String> terminosExcluidos,
            String explicacion) {
        servicios.values().stream()
                .filter(disponible -> {
                    String textoServicio = textoServicio(disponible);
                    return coincideConAlguno(textoServicio, terminosServicio)
                            && !coincideConAlguno(textoServicio, terminosExcluidos);
                })
                .forEach(disponible -> coincidencias.put(
                        disponible.servicio().getId(), new CoincidenciaCatalogo(10, explicacion)));
    }

    private void agregarCoincidenciasTextuales(
            String sintomas,
            Map<Long, ServicioDisponible> servicios,
            Map<Long, CoincidenciaCatalogo> coincidencias) {
        Set<String> terminosSintomas = extraerTerminos(sintomas);
        servicios.values().forEach(disponible -> {
            Set<String> terminosServicio = extraerTerminos(textoServicio(disponible));
            long cantidad = terminosSintomas.stream().filter(terminosServicio::contains).count();
            if (cantidad > 0) {
                coincidencias.put(
                        disponible.servicio().getId(),
                        new CoincidenciaCatalogo(
                                (int) cantidad,
                                "El nombre o la descripción del servicio coincide con términos de los síntomas; esta coincidencia no confirma pertinencia clínica."));
            }
        });
    }

    private String textoServicio(ServicioDisponible disponible) {
        return normalizar(disponible.servicio().getNombre() + " "
                + (disponible.servicio().getDescripcion() == null ? "" : disponible.servicio().getDescripcion())
                + " " + (disponible.servicio().getCategoria() == null ? "" : disponible.servicio().getCategoria()));
    }

    private boolean coincideConAlguno(String textoNormalizado, List<String> frases) {
        return frases.stream().map(this::normalizar).anyMatch(textoNormalizado::contains);
    }

    private Set<String> extraerTerminos(String texto) {
        String normalizado = normalizar(texto);
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

    private String normalizar(String texto) {
        return Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
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

    private record CoincidenciaCatalogo(int puntaje, String explicacion) {
    }
}
