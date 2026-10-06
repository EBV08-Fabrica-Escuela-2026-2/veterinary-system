package com.veterinaria.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DisponibilidadDiaDTO {

    private Long servicioId;
    private String servicioNombre;
    private Integer duracionMinutos;
    private Long veterinarioId;
    private String veterinarioNombre;
    private LocalDate fecha;
    private boolean hayDisponibilidad;
    private List<FranjaHorariaDTO> horariosDisponibles = new ArrayList<>();
    private String mensaje;
    private LocalDate fechaDisponibleMasCercana;

    public DisponibilidadDiaDTO() {
    }

    public Long getServicioId() {
        return servicioId;
    }

    public void setServicioId(Long servicioId) {
        this.servicioId = servicioId;
    }

    public String getServicioNombre() {
        return servicioNombre;
    }

    public void setServicioNombre(String servicioNombre) {
        this.servicioNombre = servicioNombre;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public Long getVeterinarioId() {
        return veterinarioId;
    }

    public void setVeterinarioId(Long veterinarioId) {
        this.veterinarioId = veterinarioId;
    }

    public String getVeterinarioNombre() {
        return veterinarioNombre;
    }

    public void setVeterinarioNombre(String veterinarioNombre) {
        this.veterinarioNombre = veterinarioNombre;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public boolean isHayDisponibilidad() {
        return hayDisponibilidad;
    }

    public void setHayDisponibilidad(boolean hayDisponibilidad) {
        this.hayDisponibilidad = hayDisponibilidad;
    }

    public List<FranjaHorariaDTO> getHorariosDisponibles() {
        return horariosDisponibles;
    }

    public void setHorariosDisponibles(List<FranjaHorariaDTO> horariosDisponibles) {
        this.horariosDisponibles = horariosDisponibles;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDate getFechaDisponibleMasCercana() {
        return fechaDisponibleMasCercana;
    }

    public void setFechaDisponibleMasCercana(LocalDate fechaDisponibleMasCercana) {
        this.fechaDisponibleMasCercana = fechaDisponibleMasCercana;
    }
}
