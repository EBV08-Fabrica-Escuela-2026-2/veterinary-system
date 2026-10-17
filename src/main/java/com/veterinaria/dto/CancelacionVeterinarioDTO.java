package com.veterinaria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelacionVeterinarioDTO {

    @NotBlank(message = "Este campo es obligatorio")
    @Size(max = 240, message = "El motivo no puede superar los 240 caracteres")
    private String motivo;
}