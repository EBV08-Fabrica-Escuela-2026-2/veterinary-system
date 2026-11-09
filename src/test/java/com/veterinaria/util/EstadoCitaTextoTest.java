package com.veterinaria.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstadoCitaTextoTest {

    @Test
    void traduceCadaEstadoAUnTextoEnEspanolClaro() {
        assertThat(EstadoCitaTexto.de("CONFIRMADA")).isEqualTo("Confirmada");
        assertThat(EstadoCitaTexto.de("ANULADA")).isEqualTo("Anulada por el cliente");
        assertThat(EstadoCitaTexto.de("CANCELADA_PROFESIONAL")).isEqualTo("Cancelada por el profesional");
        assertThat(EstadoCitaTexto.de("COMPLETADA")).isEqualTo("Completada");
        assertThat(EstadoCitaTexto.de("NO_ASISTIO")).isEqualTo("No asistió");
    }

    @Test
    void estadoDesconocidoOVacioNoRompe() {
        assertThat(EstadoCitaTexto.de("OTRO")).isEqualTo("OTRO");
        assertThat(EstadoCitaTexto.de(null)).isNull();
    }
}
