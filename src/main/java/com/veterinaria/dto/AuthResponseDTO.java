package com.veterinaria.dto;

import java.util.List;

public class AuthResponseDTO {

    private String mensaje;
    private String token;
    private String tipoToken;
    private Long expiraEnMs;
    /** Rol principal (ADMINISTRADOR, VETERINARIO o CLIENTE) para redirigir al panel correspondiente. */
    private String rol;
    private List<String> roles;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(String mensaje, String token, String tipoToken, Long expiraEnMs) {
        this(mensaje, token, tipoToken, expiraEnMs, null, null);
    }

    public AuthResponseDTO(String mensaje, String token, String tipoToken, Long expiraEnMs, String rol,
            List<String> roles) {
        this.mensaje = mensaje;
        this.token = token;
        this.tipoToken = tipoToken;
        this.expiraEnMs = expiraEnMs;
        this.rol = rol;
        this.roles = roles;
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

    public String getRol() {
        return rol;
    }

    public List<String> getRoles() {
        return roles;
    }
}
