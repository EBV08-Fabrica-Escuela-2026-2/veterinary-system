package com.veterinaria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.veterinaria.dto.CitaReporteFila;
import com.veterinaria.dto.UsoServiciosReporteDTO;
import com.veterinaria.repository.ReporteCitasRepository;
import com.veterinaria.service.impl.ReporteServicioServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReporteServicioServiceImplTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-05T15:00:00Z"), ZoneId.of("America/Bogota"));

    @Mock
    private ReporteCitasRepository reporteRepository;

    private ReporteServicioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReporteServicioServiceImpl(reporteRepository, clock);
        when(reporteRepository.nombresServicios())
                .thenReturn(List.of("Consulta General", "Odontología", "Vacunación"));
    }

    private CitaReporteFila fila(String servicio, String estado, String precio, int dia) {
        return new CitaReporteFila(servicio, estado, new BigDecimal(precio), LocalDateTime.of(2026, 11, dia, 9, 0));
    }

    private void citasDelMes(CitaReporteFila... filas) {
        when(reporteRepository.filasDelPeriodo(any(), any())).thenReturn(List.of(filas));
    }

    private void assertDinero(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }

    @Test
    void ordenaPorVolumenYCalculaTotales() {
        citasDelMes(
                fila("Vacunación", "COMPLETADA", "45000", 4),
                fila("Consulta General", "COMPLETADA", "85000", 2),
                fila("Consulta General", "COMPLETADA", "85000", 3),
                fila("Consulta General", "ANULADA", "85000", 10),
                fila("Odontología", "CONFIRMADA", "150000", 20),
                fila("Odontología", "NO_ASISTIO", "150000", 21));

        UsoServiciosReporteDTO r = service.usoDeServicios(null, null);

        assertEquals("2026-11", r.periodo());
        assertEquals("Noviembre de 2026", r.periodoTexto());
        assertEquals(6, r.citasSolicitadas());
        assertEquals(3, r.atencionesRealizadas());
        assertEquals(50, r.tasaCumplimiento());
        assertDinero("215000", r.ingresosGenerados());

        assertEquals(3, r.servicios().size());
        UsoServiciosReporteDTO.ServicioUso primero = r.servicios().get(0);
        assertEquals(1, primero.posicion());
        assertEquals("Consulta General", primero.servicio());
        assertEquals(3, primero.solicitudes());
        assertEquals(2, primero.atencionesRealizadas());
        assertEquals(67, primero.tasaAtencion());
        assertDinero("170000", primero.valorFacturado());
        assertEquals("Odontología", r.servicios().get(1).servicio());
        assertEquals("Vacunación", r.servicios().get(2).servicio());
    }

    @Test
    void filtraPorServicio_sinDistinguirMayusculas() {
        citasDelMes(
                fila("Vacunación", "COMPLETADA", "45000", 4),
                fila("Consulta General", "COMPLETADA", "85000", 2));

        UsoServiciosReporteDTO r = service.usoDeServicios(null, "vacunación");

        assertEquals("vacunación", r.servicioFiltro());
        assertEquals(1, r.citasSolicitadas());
        assertEquals(1, r.servicios().size());
        assertEquals("Vacunación", r.servicios().get(0).servicio());
        assertDinero("45000", r.ingresosGenerados());
        assertEquals(3, r.serviciosDisponibles().size());
    }

    @Test
    void tendenciaSemanal_agrupaPorBloquesDeSieteDias() {
        citasDelMes(
                fila("Consulta General", "COMPLETADA", "85000", 2),
                fila("Consulta General", "COMPLETADA", "85000", 9),
                fila("Consulta General", "COMPLETADA", "85000", 16),
                fila("Consulta General", "COMPLETADA", "85000", 23),
                fila("Consulta General", "COMPLETADA", "85000", 30));

        UsoServiciosReporteDTO r = service.usoDeServicios(YearMonth.of(2026, 11), null);

        assertEquals(4, r.tendencia().size());
        assertEquals("Semana 1", r.tendencia().get(0).etiqueta());
        assertEquals(List.of(1L, 1L, 1L, 2L),
                r.tendencia().stream().map(UsoServiciosReporteDTO.SemanaTendencia::solicitudes).toList());
    }

    @Test
    void mesSinCitas_devuelveCerosYListasVacias() {
        citasDelMes();

        UsoServiciosReporteDTO r = service.usoDeServicios(YearMonth.of(2027, 1), null);

        assertEquals("Enero de 2027", r.periodoTexto());
        assertEquals(0, r.citasSolicitadas());
        assertEquals(0, r.atencionesRealizadas());
        assertEquals(0, r.tasaCumplimiento());
        assertDinero("0", r.ingresosGenerados());
        assertTrue(r.servicios().isEmpty());
        assertEquals(4, r.tendencia().size());
    }

    @Test
    void servicioInexistente_esRechazado() {
        citasDelMes();

        assertThrows(IllegalArgumentException.class, () -> service.usoDeServicios(null, "Cirugía"));
    }
}