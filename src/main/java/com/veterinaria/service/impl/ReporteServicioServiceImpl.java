package com.veterinaria.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.dto.CitaReporteFila;
import com.veterinaria.dto.UsoServiciosReporteDTO;
import com.veterinaria.repository.ReporteCitasRepository;
import com.veterinaria.service.ReporteServicioService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReporteServicioServiceImpl implements ReporteServicioService {

    private static final String ESTADO_COMPLETADA = "COMPLETADA";
    private static final Locale ES_CO = Locale.of("es", "CO");
    private static final int SEMANAS = 4;

    private final ReporteCitasRepository reporteRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public UsoServiciosReporteDTO usoDeServicios(YearMonth mes, String servicio) {
        YearMonth periodo = mes != null ? mes : YearMonth.now(clock);
        String filtro = (servicio == null || servicio.isBlank()) ? null : servicio.trim();

        List<String> disponibles = reporteRepository.nombresServicios();
        if (filtro != null && disponibles.stream().noneMatch(nombre -> nombre.equalsIgnoreCase(filtro))) {
            throw new IllegalArgumentException("El servicio indicado no existe en el catálogo");
        }

        LocalDateTime desde = periodo.atDay(1).atStartOfDay();
        LocalDateTime hasta = periodo.plusMonths(1).atDay(1).atStartOfDay();
        List<CitaReporteFila> filas = reporteRepository.filasDelPeriodo(desde, hasta).stream()
                .filter(fila -> filtro == null || fila.servicio().equalsIgnoreCase(filtro))
                .toList();

        long solicitadas = filas.size();
        long atenciones = filas.stream().filter(this::esCompletada).count();
        BigDecimal ingresos = sumarPrecios(filas.stream().filter(this::esCompletada));

        Map<String, List<CitaReporteFila>> porServicio = filas.stream()
                .collect(Collectors.groupingBy(CitaReporteFila::servicio));
        List<Map.Entry<String, List<CitaReporteFila>>> ordenados = new ArrayList<>(porServicio.entrySet());
        ordenados.sort((a, b) -> {
            int porVolumen = Integer.compare(b.getValue().size(), a.getValue().size());
            return porVolumen != 0 ? porVolumen : a.getKey().compareToIgnoreCase(b.getKey());
        });

        List<UsoServiciosReporteDTO.ServicioUso> servicios = new ArrayList<>();
        int posicion = 1;
        for (Map.Entry<String, List<CitaReporteFila>> entrada : ordenados) {
            List<CitaReporteFila> grupo = entrada.getValue();
            long solicitudes = grupo.size();
            long atendidas = grupo.stream().filter(this::esCompletada).count();
            servicios.add(new UsoServiciosReporteDTO.ServicioUso(
                    posicion++,
                    entrada.getKey(),
                    solicitudes,
                    atendidas,
                    porcentaje(atendidas, solicitudes),
                    sumarPrecios(grupo.stream().filter(this::esCompletada))));
        }

        long[] porSemana = new long[SEMANAS];
        filas.forEach(fila -> porSemana[Math.min((fila.fechaHoraInicio().getDayOfMonth() - 1) / 7, SEMANAS - 1)]++);
        List<UsoServiciosReporteDTO.SemanaTendencia> tendencia = new ArrayList<>();
        for (int i = 0; i < SEMANAS; i++) {
            tendencia.add(new UsoServiciosReporteDTO.SemanaTendencia(i + 1, "Semana " + (i + 1), porSemana[i]));
        }

        return new UsoServiciosReporteDTO(
                periodo.toString(),
                textoPeriodo(periodo),
                filtro,
                solicitadas,
                atenciones,
                porcentaje(atenciones, solicitadas),
                ingresos,
                servicios,
                tendencia,
                disponibles);
    }

    private boolean esCompletada(CitaReporteFila fila) {
        return ESTADO_COMPLETADA.equals(fila.estado());
    }

    private BigDecimal sumarPrecios(Stream<CitaReporteFila> filas) {
        return filas.map(CitaReporteFila::precio)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int porcentaje(long parte, long total) {
        if (total == 0) {
            return 0;
        }
        return BigDecimal.valueOf(parte * 100)
                .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private String textoPeriodo(YearMonth periodo) {
        String mes = periodo.getMonth().getDisplayName(TextStyle.FULL, ES_CO);
        return Character.toUpperCase(mes.charAt(0)) + mes.substring(1) + " de " + periodo.getYear();
    }
}