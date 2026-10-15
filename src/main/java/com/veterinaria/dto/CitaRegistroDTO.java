package com.veterinaria.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CitaRegistroDTO {

    @NotNull(message = "El id de la mascota es obligatorio")
    private Long mascotaId;

    @NotNull(message = "El id del veterinario es obligatorio")
    private Long veterinarioId;

    @NotNull(message = "El id del servicio es obligatorio")
    private Long servicioId;

    @NotNull(message = "La fecha y hora de la cita son obligatorias")
    @Future(message = "La fecha y hora de la cita deben ser futuras")
    private LocalDateTime fechaHoraInicio;

    public Long getMascotaId() {
        return mascotaId;
    }

    public void setMascotaId(Long mascotaId) {
        this.mascotaId = mascotaId;
    }

    public Long getVeterinarioId() {
        return veterinarioId;
    }

    public void setVeterinarioId(Long veterinarioId) {
        this.veterinarioId = veterinarioId;
    }

    public Long getServicioId() {
        return servicioId;
    }

    public void setServicioId(Long servicioId) {
        this.servicioId = servicioId;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }
}