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
@Schema(description = "Datos de un cliente registrado")
public class ClienteResponseDTO {
    @Schema(description = "Identificador único del cliente", example = "1")
    private Long id;

    @Schema(description = "Nombre completo del cliente", example = "Juana Pérez")
    private String nombre;

    @Schema(description = "Documento de identidad del cliente", example = "1023456789")
    private String documentoIdentidad;

    @Schema(description = "Teléfono de contacto", example = "3101234567")
    private String telefono;

    @Schema(description = "Correo electrónico", example = "juana@example.com")
    private String correo;

    @Schema(description = "Dirección de residencia", example = "Calle 10 #5-20, Bogotá")
    private String direccion;

    @Schema(description = "Estado del cliente", example = "true")
    private Boolean activo;
}
