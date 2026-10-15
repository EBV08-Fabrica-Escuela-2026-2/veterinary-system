package com.veterinaria.dto;

import java.time.LocalDateTime;

public class CitaClienteDTO {

    private Long id;
    private String mascotaNombre;
    private String veterinarioNombre;
    private String servicioNombre;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private String estado;
    private String etiquetaEstado;
    private boolean cupoLiberado;
    private LocalDateTime createdAt;

    public CitaClienteDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMascotaNombre() {
        return mascotaNombre;
    }

    public void setMascotaNombre(String mascotaNombre) {
        this.mascotaNombre = mascotaNombre;
    }

    public String getVeterinarioNombre() {
        return veterinarioNombre;
    }

    public void setVeterinarioNombre(String veterinarioNombre) {
        this.veterinarioNombre = veterinarioNombre;
    }

    public String getServicioNombre() {
        return servicioNombre;
    }

    public void setServicioNombre(String servicioNombre) {
        this.servicioNombre = servicioNombre;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
