package com.veterinaria.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class CitaDetalleDTO {
     private Long id;
    private String codigo;
    private String estado;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private boolean puedeCancelarse;
    private MascotaInfo mascota;
    private ServicioInfo servicio;
    private ProfesionalInfo profesional;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MascotaInfo {
        private Long id;
        private String nombre;
        private String especie;
        private String raza;
        private Integer edad;
        private String sexo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicioInfo {
        private Long id;
        private String nombre;
        private String categoria;
        private BigDecimal precio;
        private Integer duracionMinutos;
        private String indicacionesPrevias;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfesionalInfo {
        private Long id;
        private String nombre;
        private String especialidad;
        private String fotoUrl;
    }
    
    
}
