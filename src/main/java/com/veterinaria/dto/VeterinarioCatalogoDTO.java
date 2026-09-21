package com.veterinaria.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Información de un veterinario en el catálogo")
public class VeterinarioCatalogoDTO {
    @Schema(description = "Identificador único del veterinario", example = "1")
    private Long id;

    @Schema(description = "Nombre completo del veterinario", example = "Dr. Carlos Pérez")
    private String nombre;

    @Schema(description = "Dirección del consultorio", example = "Calle 10 #5-20, Bogotá")
    private String direccion;

    @Schema(description = "Teléfono de contacto", example = "310-123-4567")
    private String telefono;

    @Schema(description = "Horario de atención", example = "8:00 AM - 6:00 PM")
    private String horarioAtencion;

    @Schema(description = "Lista de servicios ofrecidos por el veterinario")
    private List<ServicioCatalogoDTO> servicios;

    @Schema(description = "Mensaje cuando el veterinario no tiene servicios publicados", example = "Sin servicios publicados")
    private String mensajeServicios;
}
