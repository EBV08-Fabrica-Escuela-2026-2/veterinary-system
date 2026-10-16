package com.veterinaria.dto;

public class AuthResponseDTO {

    private String mensaje;
    private String token;
    private String tipoToken;
    private Long expiraEnMs;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(String mensaje, String token, String tipoToken, Long expiraEnMs) {
        this.mensaje = mensaje;
        this.token = token;
        this.tipoToken = tipoToken;
        this.expiraEnMs = expiraEnMs;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getToken() {
        return token;
    }

    public String getTipoToken() {
        return tipoToken;
    }

    public Long getExpiraEnMs() {
        return expiraEnMs;
    }
}