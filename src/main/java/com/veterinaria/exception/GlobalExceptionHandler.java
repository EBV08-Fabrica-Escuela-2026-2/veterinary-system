package com.veterinaria.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.veterinaria.exception.ProblemDetailFactory.ErrorCampo;

import jakarta.validation.ConstraintViolationException;

/**
 * Todas las respuestas de error salen en formato RFC 7807 (Problem Details),
 * con el mensaje en español y, cuando aplica, el campo que falló.
 * Ejemplo:
 * { "type": "about:blank", "title": "Datos no válidos", "status": 400,
 *   "detail": "El motivo debe tener mínimo 10 caracteres",
 *   "mensaje": "El motivo debe tener mínimo 10 caracteres", "campo": "motivo" }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String TITULO_NO_ENCONTRADO = "Recurso no encontrado";
    private static final String TITULO_NO_AUTENTICADO = "No autenticado";
    private static final String TITULO_ACCESO_DENEGADO = "Acceso denegado";
    private static final String TITULO_CONFLICTO = "Conflicto con el estado actual";
    private static final String TITULO_DATOS_NO_VALIDOS = "Datos no válidos";

    // ---------- 404 ----------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, TITULO_NO_ENCONTRADO, ex.getMessage(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleRutaNoEncontrada(NoResourceFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, TITULO_NO_ENCONTRADO, "La dirección solicitada no existe", null);
    }

    // ---------- 401 / 403 ----------

    @ExceptionHandler(ClienteNoAutenticadoException.class)
    public ResponseEntity<ProblemDetail> handleClienteNoAutenticado(ClienteNoAutenticadoException ex) {
        return responder(HttpStatus.UNAUTHORIZED, TITULO_NO_AUTENTICADO, ex.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return responder(HttpStatus.FORBIDDEN, TITULO_ACCESO_DENEGADO,
                "No tienes permisos para realizar esta acción", null);
    }

    // ---------- 409 ----------

    @ExceptionHandler(CitaNoDisponibleException.class)
    public ResponseEntity<ProblemDetail> handleCitaNoDisponible(CitaNoDisponibleException ex) {
        return responder(HttpStatus.CONFLICT, TITULO_CONFLICTO, ex.getMessage(), null);
    }

    @ExceptionHandler(CancelacionNoPermitidaException.class)
    public ResponseEntity<ProblemDetail> handleCancelacionNoPermitida(CancelacionNoPermitidaException ex) {
        return responder(HttpStatus.CONFLICT, TITULO_CONFLICTO, ex.getMessage(), null);
    }

    @ExceptionHandler(FranjaTraslapadaException.class)
    public ResponseEntity<ProblemDetail> handleFranjaTraslapada(FranjaTraslapadaException ex) {
        return responder(HttpStatus.CONFLICT, TITULO_CONFLICTO, ex.getMessage(), null);
    }

    // ---------- 400 ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(this::aErrorCampo)
                .toList();
        if (errores.isEmpty()) {
            return responder(HttpStatus.BAD_REQUEST, TITULO_DATOS_NO_VALIDOS,
                    "Los campos obligatorios deben completarse", null);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ProblemDetailFactory.crearConErrores(HttpStatus.BAD_REQUEST, TITULO_DATOS_NO_VALIDOS, errores));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorCampo> errores = ex.getConstraintViolations().stream()
                .map(cv -> {
                    String campo = ultimoNodo(cv.getPropertyPath().toString());
                    return new ErrorCampo(campo, ProblemDetailFactory.mensajeClaro(campo, cv.getMessage()));
                })
                .toList();
        if (errores.isEmpty()) {
            return responder(HttpStatus.BAD_REQUEST, TITULO_DATOS_NO_VALIDOS,
                    "Los datos enviados no son válidos", null);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ProblemDetailFactory.crearConErrores(HttpStatus.BAD_REQUEST, TITULO_DATOS_NO_VALIDOS, errores));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMensajeNoLegible(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, TITULO_DATOS_NO_VALIDOS,
                "El cuerpo de la solicitud no es válido", null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleParametroFaltante(MissingServletRequestParameterException ex) {
        String campo = ex.getParameterName();
        return responder(HttpStatus.BAD_REQUEST, TITULO_DATOS_NO_VALIDOS,
                "Falta el dato obligatorio " + campo, campo);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        String campo = ex.getName();
        return responder(HttpStatus.BAD_REQUEST, TITULO_DATOS_NO_VALIDOS,
                "El valor enviado para " + campo + " no tiene el formato correcto", campo);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        return responder(HttpStatus.BAD_REQUEST, TITULO_DATOS_NO_VALIDOS, ex.getMessage(), null);
    }

    // ---------- 405 / 415 ----------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMetodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido",
                "Esta dirección no admite la operación " + ex.getMethod(), null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleTipoContenidoNoSoportado(HttpMediaTypeNotSupportedException ex) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato no soportado",
                "El formato de los datos enviados no es compatible; usa JSON", null);
    }

    // ---------- 500 ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneral(Exception ex) {
        logger.error("Excepción no manejada: ", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor",
                "Ocurrió un error inesperado. Intenta de nuevo más tarde", null);
    }

    // ---------- utilidades ----------

    private ResponseEntity<ProblemDetail> responder(HttpStatus status, String titulo, String mensaje, String campo) {
        return ResponseEntity.status(status).body(ProblemDetailFactory.crear(status, titulo, mensaje, campo));
    }

    private ErrorCampo aErrorCampo(FieldError error) {
        String campo = error.getField();
        return new ErrorCampo(campo, ProblemDetailFactory.mensajeClaro(campo, error.getDefaultMessage()));
    }

    /** "registrar.arg0.motivo" -> "motivo" */
    private String ultimoNodo(String ruta) {
        int punto = ruta.lastIndexOf('.');
        return punto >= 0 ? ruta.substring(punto + 1) : ruta;
    }
}
