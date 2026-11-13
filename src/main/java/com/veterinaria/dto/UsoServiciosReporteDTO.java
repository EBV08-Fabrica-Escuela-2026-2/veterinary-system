package com.veterinaria.dto;

import java.math.BigDecimal;
import java.util.List;

public record UsoServiciosReporteDTO(
        String periodo,
        String periodoTexto,
        String servicioFiltro,
        long citasSolicitadas,
        long atencionesRealizadas,
        int tasaCumplimiento,
        BigDecimal ingresosGenerados,
        List<ServicioUso> servicios,
        List<SemanaTendencia> tendencia,
        List<String> serviciosDisponibles) {

    /** Una fila de la tabla "Servicios por volumen de solicitudes". */
    public record ServicioUso(
            int posicion,
            String servicio,
            long solicitudes,
            long atencionesRealizadas,
            int tasaAtencion,
            BigDecimal valorFacturado) {
    }

    /** Un punto de la gráfica "Tendencia de solicitudes". */
    public record SemanaTendencia(
            int semana,
            String etiqueta,
            long solicitudes) {
    }
}