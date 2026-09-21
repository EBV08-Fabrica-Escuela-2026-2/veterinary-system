package com.veterinaria.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Información de un servicio en el catálogo")
public class ServicioCatalogoDTO {
    @Schema(description = "Identificador único del servicio", example = "1")
    private Long id;

    @Schema(description = "Nombre del servicio", example = "Consulta General")
    private String nombre;

    @Schema(description = "Descripción breve del servicio", example = "Revisión completa del paciente")
    private String descripcion;

    @Schema(description = "Precio del servicio en COP (número entero)", example = "85000")
    private Long precio;

    @Schema(description = "Duración del servicio en minutos", example = "30")
    private Integer duracionMinutos;
}
