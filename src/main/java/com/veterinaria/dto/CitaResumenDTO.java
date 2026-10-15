package com.veterinaria.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class CitaResumenDTO {
    private Long id;
    private String codigo;
    private String estado;
    private LocalDateTime fechaHoraInicio;
    private String servicioNombre;
    private String mascotaNombre;
    
}
