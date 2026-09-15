package com.veterinaria.dto;

import jakarta.validation.constraints.*;

public class VeterinarioRegistroDTO {
    @NotBlank(message = "Este campo es obligatorio")
    private String nombre;

    @NotBlank(message = "Este campo es obligatorio")
    @Pattern(regexp = "\\d+", message = "El documento solo debe contener números")
    private String documentoIdentidad;

    @NotBlank(message = "Este campo es obligatorio")
    @Pattern(regexp = "\\d{10}", message = "El número celular no es válido")
    private String telefono;

    @NotBlank(message = "Este campo es obligatorio")
    @Email(message = "El correo no es válido")
    private String correo;

    private String direccion;

    private String horarioAtencion;

    // Getters y setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public void setDocumentoIdentidad(String documentoIdentidad) { this.documentoIdentidad = documentoIdentidad; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getHorarioAtencion() { return horarioAtencion; }
    public void setHorarioAtencion(String horarioAtencion) { this.horarioAtencion = horarioAtencion; }
    
}
