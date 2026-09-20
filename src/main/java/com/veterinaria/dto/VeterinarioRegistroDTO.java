package com.veterinaria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class VeterinarioRegistroDTO {
    @NotBlank(message = "Este campo es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    @NotBlank(message = "Este campo es obligatorio")
    @Pattern(regexp = "\\d+", message = "El documento solo debe contener números")
    @Size(max = 20, message = "El documento no puede superar los 20 caracteres")
    private String documentoIdentidad;

    @NotBlank(message = "Este campo es obligatorio")
    @Pattern(regexp = "\\d{10}", message = "El número celular no es válido")
    private String telefono;

    @NotBlank(message = "Este campo es obligatorio")
    @Email(message = "El correo no es válido")
    @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
    private String correo;

    @NotBlank(message = "Este campo es obligatorio")
    private String tipoDocumento;

    @NotBlank(message = "Este campo es obligatorio")
    @Size(max = 50, message = "La tarjeta profesional no puede superar los 50 caracteres")
    private String tarjetaProfesional;

    @NotBlank(message = "Este campo es obligatorio")
    private String especialidad;

    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    private String direccion;

    @Size(max = 100, message = "El horario de atención no puede superar los 100 caracteres")
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

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getTarjetaProfesional() { return tarjetaProfesional; }
    public void setTarjetaProfesional(String tarjetaProfesional) { this.tarjetaProfesional = tarjetaProfesional; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getHorarioAtencion() { return horarioAtencion; }
    public void setHorarioAtencion(String horarioAtencion) { this.horarioAtencion = horarioAtencion; }
    
}
