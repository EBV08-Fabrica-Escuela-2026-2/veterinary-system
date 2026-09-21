package com.veterinaria.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ServicioRegistroDTO {

    @NotNull(message = "Este campo es obligatorio")
    private Long veterinarioId;

    @NotBlank(message = "Este campo es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    private String descripcion;

    @NotNull(message = "Este campo es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a cero")
    private BigDecimal precio;

    @NotNull(message = "Este campo es obligatorio")
    @Min(value = 1, message = "La duración debe ser de al menos 1 minuto")
    private Integer duracionMinutos;

    // Getters y setters
    public Long getVeterinarioId() { return veterinarioId; }
    public void setVeterinarioId(Long veterinarioId) { this.veterinarioId = veterinarioId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
}
