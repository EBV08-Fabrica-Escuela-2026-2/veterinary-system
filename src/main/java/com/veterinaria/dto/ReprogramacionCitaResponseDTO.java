package com.veterinaria.dto;

import java.time.LocalDateTime;

public class ReprogramacionCitaResponseDTO {

    private String mensaje;
    private LocalDateTime fechaHoraAnterior;
    private CitaClienteDTO cita;

    public ReprogramacionCitaResponseDTO() {
    }

    public ReprogramacionCitaResponseDTO(String mensaje, LocalDateTime fechaHoraAnterior, CitaClienteDTO cita) {
        this.mensaje = mensaje;
        this.fechaHoraAnterior = fechaHoraAnterior;
        this.cita = cita;
    }

    public String getMensaje() {
        return mensaje;
    }

    public LocalDateTime getFechaHoraAnterior() {
        return fechaHoraAnterior;
    }

    public CitaClienteDTO getCita() {
        return cita;
    }
}