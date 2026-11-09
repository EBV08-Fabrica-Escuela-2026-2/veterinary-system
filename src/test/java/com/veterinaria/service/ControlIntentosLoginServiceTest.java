package com.veterinaria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.veterinaria.exception.DemasiadosIntentosException;

class ControlIntentosLoginServiceTest {

    private RelojAjustable reloj;
    private ControlIntentosLoginService control;

    @BeforeEach
    void preparar() {
        reloj = new RelojAjustable(Instant.parse("2026-11-09T15:00:00Z"));
        control = new ControlIntentosLoginService(reloj, 5, 15);
    }

    @Test
    void cuatroFallosConsecutivosTodaviaPermitenIntentar() {
        fallar("ana@vet.com", "10.0.0.1", 4);

        assertThatCode(() -> control.verificarPermitido("ana@vet.com", "10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    void elSextoIntentoTrasCincoFallosSeBloqueaQuinceMinutos() {
        fallar("ana@vet.com", "10.0.0.1", 5);

        assertThatThrownBy(() -> control.verificarPermitido("ana@vet.com", "10.0.0.1"))
                .isInstanceOf(DemasiadosIntentosException.class)
                .hasMessage("Demasiados intentos fallidos. Intenta nuevamente en 15 minutos.")
                .extracting(error -> ((DemasiadosIntentosException) error).getSegundosRestantes())
                .isEqualTo(900L);
    }

    @Test
    void elBloqueoPorCuentaAplicaDesdeCualquierIp() {
        fallar("ana@vet.com", "10.0.0.1", 5);

        assertThatThrownBy(() -> control.verificarPermitido("ANA@vet.com", "198.51.100.7"))
                .isInstanceOf(DemasiadosIntentosException.class);
    }

    @Test
    void elBloqueoPorIpAplicaAunqueSeCambieDeCorreo() {
        for (int i = 0; i < 5; i++) {
            control.registrarFallo("correo" + i + "@vet.com", "10.0.0.9");
        }

        assertThatThrownBy(() -> control.verificarPermitido("otro@vet.com", "10.0.0.9"))
                .isInstanceOf(DemasiadosIntentosException.class);
        assertThatCode(() -> control.verificarPermitido("otro@vet.com", "10.0.0.10")).doesNotThrowAnyException();
    }

    @Test
    void elTiempoRestanteDisminuyeYElBloqueoVenceALosQuinceMinutos() {
        fallar("ana@vet.com", "10.0.0.1", 5);

        reloj.avanzar(Duration.ofMinutes(10));
        assertThatThrownBy(() -> control.verificarPermitido("ana@vet.com", "10.0.0.1"))
                .hasMessage("Demasiados intentos fallidos. Intenta nuevamente en 5 minutos.");

        reloj.avanzar(Duration.ofMinutes(5));
        assertThatCode(() -> control.verificarPermitido("ana@vet.com", "10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    void trasVencerElBloqueoElConteoEmpiezaDeNuevo() {
        fallar("ana@vet.com", "10.0.0.1", 5);
        reloj.avanzar(Duration.ofMinutes(16));

        fallar("ana@vet.com", "10.0.0.1", 1);

        assertThatCode(() -> control.verificarPermitido("ana@vet.com", "10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    void unInicioDeSesionExitosoReiniciaLosFallosConsecutivos() {
        fallar("ana@vet.com", "10.0.0.1", 4);
        control.registrarExito("ana@vet.com", "10.0.0.1");
        fallar("ana@vet.com", "10.0.0.1", 4);

        assertThatCode(() -> control.verificarPermitido("ana@vet.com", "10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    void fallosSeparadosPorMasDeLaVentanaNoSeConsideranConsecutivos() {
        fallar("ana@vet.com", "10.0.0.1", 4);
        reloj.avanzar(Duration.ofMinutes(20));
        fallar("ana@vet.com", "10.0.0.1", 1);

        assertThatCode(() -> control.verificarPermitido("ana@vet.com", "10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    void ipOCorreoVaciosNoRompenElControl() {
        fallar(null, null, 6);

        assertThatCode(() -> control.verificarPermitido(null, "")).doesNotThrowAnyException();
    }

    @Test
    void rechazaUnaPoliticaInvalida() {
        assertThatThrownBy(() -> new ControlIntentosLoginService(reloj, 0, 15))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new ControlIntentosLoginService(reloj, 1, 1)).isNotNull();
    }

    private void fallar(String correo, String ip, int veces) {
        for (int i = 0; i < veces; i++) {
            control.registrarFallo(correo, ip);
        }
    }

    private static final class RelojAjustable extends Clock {

        private Instant ahora;

        private RelojAjustable(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
