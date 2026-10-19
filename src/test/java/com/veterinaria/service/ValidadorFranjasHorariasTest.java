package com.veterinaria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.veterinaria.exception.FranjaTraslapadaException;
import com.veterinaria.model.FranjaHoraria;

class ValidadorFranjasHorariasTest {

    private final ValidadorFranjasHorarias validador = new ValidadorFranjasHorarias();

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource(delimiter = '|', value = {
            "contiguas (08-10 y 10-12)            | 08:00 | 10:00 | 10:00 | 12:00 | false",
            "contiguas en orden inverso           | 10:00 | 12:00 | 08:00 | 10:00 | false",
            "separadas (08-10 y 11-12)            | 08:00 | 10:00 | 11:00 | 12:00 | false",
            "traslape parcial por el fin          | 08:00 | 10:00 | 09:00 | 11:00 | true",
            "traslape parcial por el inicio       | 09:00 | 11:00 | 08:00 | 10:00 | true",
            "una contenida en la otra             | 08:00 | 12:00 | 09:00 | 10:00 | true",
            "la otra contiene a la primera        | 09:00 | 10:00 | 08:00 | 12:00 | true",
            "idénticas                            | 08:00 | 10:00 | 08:00 | 10:00 | true",
            "mismo inicio y distinto fin          | 08:00 | 10:00 | 08:00 | 09:00 | true",
            "mismo fin y distinto inicio          | 08:00 | 10:00 | 09:00 | 10:00 | true"
    })
    void seTraslapa_aplicaIntervalosSemiabiertos(
            String descripcion,
            LocalTime inicio1,
            LocalTime fin1,
            LocalTime inicio2,
            LocalTime fin2,
            boolean esperado) {
        assertThat(validador.seTraslapa(inicio1, fin1, inicio2, fin2)).isEqualTo(esperado);
    }

    @Test
    void validarSinTraslapes_aceptaFranjasContiguasYSeparadas() {
        assertThatCode(() -> validador.validarSinTraslapes(List.of(
                franja("08:00", "10:00"), franja("10:00", "12:00"), franja("14:00", "16:00"))))
                .doesNotThrowAnyException();
    }

    @Test
    void validarSinTraslapes_detectaTraslapeDespuesDeOrdenarUnaListaDesordenada() {
        List<FranjaHoraria> desordenadas = List.of(franja("14:00", "16:00"), franja("08:00", "10:00"),
                franja("09:00", "12:00"));

        assertThatThrownBy(() -> validador.validarSinTraslapes(desordenadas))
                .isInstanceOf(FranjaTraslapadaException.class)
                .hasMessage(ValidadorFranjasHorarias.MENSAJE_TRASLAPE);
    }

    @Test
    void validarSinTraslapes_aceptaUnaListaDesordenadaSinTraslapes() {
        assertThatCode(() -> validador.validarSinTraslapes(List.of(
                franja("14:00", "16:00"), franja("08:00", "10:00"), franja("10:00", "12:00"))))
                .doesNotThrowAnyException();
    }

    @Test
    void validarSinTraslapes_detectaUnaFranjaLargaQueEnvuelveVariasCortas() {
        assertThatThrownBy(() -> validador.validarSinTraslapes(List.of(
                franja("08:00", "20:00"), franja("09:00", "10:00"), franja("11:00", "12:00"))))
                .isInstanceOf(FranjaTraslapadaException.class);
    }

    @Test
    void validarSinTraslapes_detectaFranjasIdenticas() {
        assertThatThrownBy(() -> validador.validarSinTraslapes(List.of(
                franja("08:00", "10:00"), franja("08:00", "10:00"))))
                .isInstanceOf(FranjaTraslapadaException.class);
    }

    @Test
    void validarSinTraslapes_aceptaListaVaciaYUnaSolaFranja() {
        assertThatCode(() -> validador.validarSinTraslapes(List.of())).doesNotThrowAnyException();
        assertThatCode(() -> validador.validarSinTraslapes(List.of(franja("08:00", "10:00"))))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource(delimiter = '|', value = {
            "inicio igual al fin     | 10:00 | 10:00",
            "fin anterior al inicio  | 10:00 | 09:00"
    })
    void validarRango_rechazaRangosInvalidos(String descripcion, LocalTime inicio, LocalTime fin) {
        assertThatThrownBy(() -> validador.validarRango(inicio, fin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cada franja debe tener una hora final posterior a la inicial");
    }

    @Test
    void validarRango_rechazaHorasNulas() {
        assertThatThrownBy(() -> validador.validarRango(null, LocalTime.of(10, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validador.validarRango(LocalTime.of(8, 0), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validador.validarRango(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validarRango_aceptaUnRangoValido() {
        assertThatCode(() -> validador.validarRango(LocalTime.of(8, 0), LocalTime.of(8, 1)))
                .doesNotThrowAnyException();
    }

    @Test
    void validarSinTraslapes_rechazaUnaFranjaConRangoInvalidoOHorasNulas() {
        assertThatThrownBy(() -> validador.validarSinTraslapes(List.of(
                franja("08:00", "10:00"), new FranjaHoraria(LocalTime.of(12, 0), LocalTime.of(11, 0)))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validador.validarSinTraslapes(List.of(
                new FranjaHoraria(null, LocalTime.of(11, 0)))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validador.validarSinTraslapes(List.of(
                new FranjaHoraria(LocalTime.of(8, 0), null))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private FranjaHoraria franja(String inicio, String fin) {
        return new FranjaHoraria(LocalTime.parse(inicio), LocalTime.parse(fin));
    }
}
