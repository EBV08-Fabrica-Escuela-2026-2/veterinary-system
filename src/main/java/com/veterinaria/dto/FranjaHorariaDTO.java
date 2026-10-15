package com.veterinaria.dto;

import java.time.LocalTime;

public class FranjaHorariaDTO {

    private LocalTime horaInicio;
    private LocalTime horaFin;

    public FranjaHorariaDTO() {
    }

    public FranjaHorariaDTO(LocalTime horaInicio, LocalTime horaFin) {
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }
}
