package com.veterinaria.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import com.veterinaria.exception.ProblemDetailFactory.ErrorCampo;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void recursoNoEncontradoDevuelve404ConMensajeEnEspanol() {
        ResponseEntity<ProblemDetail> respuesta = handler
                .handleResourceNotFound(new ResourceNotFoundException("Cita no encontrada con id: 7"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ProblemDetail cuerpo = respuesta.getBody();
        assertThat(cuerpo).isNotNull();
        assertThat(cuerpo.getStatus()).isEqualTo(404);
        assertThat(cuerpo.getDetail()).isEqualTo("Cita no encontrada con id: 7");
        Map<String, Object> extra = cuerpo.getProperties();
        assertThat(extra).containsEntry("mensaje", "Cita no encontrada con id: 7");
        assertThat(extra).containsKey("timestamp");
        assertThat(extra).doesNotContainKey("campo");
    }

    @Test
    void accesoDenegadoDevuelve403ConMensajeFijoEnEspanol() {
        ResponseEntity<ProblemDetail> respuesta = handler
                .handleAccessDenied(new AccessDeniedException("Access Denied"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody().getProperties())
                .containsEntry("mensaje", "No tienes permisos para realizar esta acción");
    }

    @Test
    void accesoDenegadoConservaElMensajePropioDeLaRegla() {
        ResponseEntity<ProblemDetail> respuesta = handler
                .handleAccessDenied(new AccessDeniedException("El cliente no se encuentra activo"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody().getDetail()).isEqualTo("El cliente no se encuentra activo");
        assertThat(respuesta.getBody().getProperties())
                .containsEntry("mensaje", "El cliente no se encuentra activo");
    }

    @Test
    void errorGeneralNoFiltraDetallesTecnicos() {
        ResponseEntity<ProblemDetail> respuesta = handler.handleGeneral(new RuntimeException("NullPointer interno"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody().getDetail()).doesNotContain("NullPointer");
    }

    @Test
    void erroresDeValidacionIncluyenCampoYListaCompleta() {
        List<ErrorCampo> errores = List.of(
                new ErrorCampo("motivo", "El motivo debe tener mínimo 10 caracteres"),
                new ErrorCampo("estado", "El estado es obligatorio"));

        ProblemDetail problema = ProblemDetailFactory.crearConErrores(HttpStatus.BAD_REQUEST, "Datos no válidos",
                errores);

        assertThat(problema.getProperties()).containsEntry("campo", "motivo");
        assertThat(problema.getProperties())
                .containsEntry("mensaje", "El motivo debe tener mínimo 10 caracteres");
        assertThat(problema.getProperties()).containsEntry("errores", errores);
    }

    @Test
    void mensajeGenericoSeVuelveEspecificoConElNombreDelCampo() {
        assertThat(ProblemDetailFactory.mensajeClaro("documentoIdentidad", "Este campo es obligatorio"))
                .isEqualTo("El campo documento identidad es obligatorio");
        assertThat(ProblemDetailFactory.mensajeClaro("motivo", "El motivo no puede superar los 240 caracteres"))
                .isEqualTo("El motivo no puede superar los 240 caracteres");
    }
}
