package com.veterinaria.exception;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Construye las respuestas de error de la API con el estándar RFC 7807
 * (Problem Details) y mensajes en español claro, pensados para que un
 * lector de pantalla o un sintetizador de voz los pueda leer sin ambigüedad.
 *
 * Campos estándar: type, title, status, detail, instance.
 * Campos propios: mensaje (igual a detail, el front ya lo usa), campo,
 * errores (lista cuando hay varios campos inválidos), error y timestamp.
 */
public final class ProblemDetailFactory {

    private static final String MENSAJE_GENERICO_OBLIGATORIO = "Este campo es obligatorio";

    private ProblemDetailFactory() {
    }

    public static ProblemDetail crear(HttpStatus status, String titulo, String mensaje) {
        return crear(status, titulo, mensaje, null);
    }

    public static ProblemDetail crear(HttpStatus status, String titulo, String mensaje, String campo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, mensaje);
        problema.setTitle(titulo);
        problema.setProperty("timestamp", LocalDateTime.now().toString());
        problema.setProperty("error", titulo);
        problema.setProperty("mensaje", mensaje);
        if (campo != null && !campo.isBlank()) {
            problema.setProperty("campo", campo);
        }
        return problema;
    }

    public static ProblemDetail crearConErrores(HttpStatus status, String titulo, List<ErrorCampo> errores) {
        ErrorCampo primero = errores.get(0);
        ProblemDetail problema = crear(status, titulo, primero.mensaje(), primero.campo());
        problema.setProperty("errores", errores);
        return problema;
    }

    /**
     * Si el mensaje de validación es el genérico ("Este campo es obligatorio")
     * lo vuelve específico para que se entienda sin ver el formulario.
     */
    public static String mensajeClaro(String campo, String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return "El campo " + nombreLegible(campo) + " no es válido";
        }
        if (MENSAJE_GENERICO_OBLIGATORIO.equals(mensaje)) {
            return "El campo " + nombreLegible(campo) + " es obligatorio";
        }
        return mensaje;
    }

    /** documentoIdentidad -> "documento identidad" */
    static String nombreLegible(String campo) {
        if (campo == null || campo.isBlank()) {
            return "indicado";
        }
        String conEspacios = campo.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        return conEspacios.toLowerCase(Locale.ROOT);
    }

    /** Un campo inválido con su mensaje en lenguaje natural. */
    public record ErrorCampo(String campo, String mensaje) {
    }
}
