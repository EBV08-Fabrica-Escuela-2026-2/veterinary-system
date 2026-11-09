package com.veterinaria.util;

/**
 * Texto en español claro para cada estado de una cita. Se envía como
 * "estadoTexto" junto al código "estado" (ej. CANCELADA_PROFESIONAL) para que
 * un lector de pantalla o un sintetizador de voz no tenga que leer códigos
 * técnicos con guiones bajos.
 */
public final class EstadoCitaTexto {

    private EstadoCitaTexto() {
    }

    public static String de(String estado) {
        if (estado == null) {
            return null;
        }
        return switch (estado) {
            case "CONFIRMADA" -> "Confirmada";
            case "ANULADA" -> "Anulada por el cliente";
            case "CANCELADA_PROFESIONAL" -> "Cancelada por el profesional";
            case "COMPLETADA" -> "Completada";
            case "NO_ASISTIO" -> "No asistió";
            default -> estado;
        };
    }
}
