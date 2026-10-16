package com.veterinaria.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public class ReprogramarCitaRequestDTO {

    @NotNull(message = "La nueva fecha es obligatoria")
    private LocalDate nuevaFecha;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime nuevaHoraInicio;

    public LocalDate getNuevaFecha() {
        return nuevaFecha;
    }

    public void setNuevaFecha(LocalDate nuevaFecha) {
        this.nuevaFecha = nuevaFecha;
    }

    public LocalTime getNuevaHoraInicio() {
        return nuevaHoraInicio;
    }

    public void setNuevaHoraInicio(LocalTime nuevaHoraInicio) {
        this.nuevaHoraInicio = nuevaHoraInicio;
    }
}