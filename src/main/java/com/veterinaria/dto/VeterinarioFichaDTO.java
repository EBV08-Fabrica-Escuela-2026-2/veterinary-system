package com.veterinaria.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class VeterinarioFichaDTO {
    private Long id;
    private String nombre;
    private String especialidad;
    private String tarjetaProfesional;
    private String fotoUrl;
    
}
