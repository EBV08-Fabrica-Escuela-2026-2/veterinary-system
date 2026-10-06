package com.veterinaria.dto;

import java.time.LocalDateTime;

public class CancelacionCitaDTO {

    private String mensaje;
    private Long idCita;
    private String estado;
    private String etiquetaEstado;
    private boolean cupoLiberado;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;

    public CancelacionCitaDTO() {
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Long getIdCita() {
        return idCita;
    }

    public void setIdCita(Long idCita) {
        this.idCita = idCita;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEtiquetaEstado() {
        return etiquetaEstado;
    }

    public void setEtiquetaEstado(String etiquetaEstado) {
        this.etiquetaEstado = etiquetaEstado;
    }

    public boolean isCupoLiberado() {
        return cupoLiberado;
    }

    public void setCupoLiberado(boolean cupoLiberado) {
        this.cupoLiberado = cupoLiberado;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(LocalDateTime fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }
}
