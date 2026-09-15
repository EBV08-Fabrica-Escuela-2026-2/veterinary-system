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
@Schema(description = "Respuesta del catálogo de servicios")
public class CatalogoResponseDTO {
    @Schema(description = "Lista de veterinarios con sus servicios", example = "[]")
    private List<VeterinarioCatalogoDTO> veterinarios;

    @Schema(description = "Moneda de los precios", example = "COP")
    private String moneda;

    @Schema(description = "Mensaje informativo cuando no hay veterinarios", example = "No hay veterinarios disponibles por el momento")
    private String mensaje;
}
