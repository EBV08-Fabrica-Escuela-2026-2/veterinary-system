package com.veterinaria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CancelacionVeterinarioDTO {

    @NotBlank(message = "Este campo es obligatorio")
    @Size(max = 240, message = "El motivo no puede superar los 240 caracteres")
    private String motivo;
        @Pattern(regexp = "LIBERAR_CUPO|BLOQUEAR_FRANJA",
            message = "La decisión sobre el horario debe ser LIBERAR_CUPO o BLOQUEAR_FRANJA")
    private String decisionHorario;
}