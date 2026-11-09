package com.veterinaria.service;

import com.veterinaria.dto.AuthResponseDTO;
import com.veterinaria.dto.ClienteResponseDTO;
import com.veterinaria.dto.LoginAuthDTO;
import com.veterinaria.dto.RegistroAuthDTO;

public interface AuthService {

    AuthResponseDTO registrar(RegistroAuthDTO solicitud);

    /**
     * Valida las credenciales de cualquier usuario (Cliente, Veterinario o Administrador)
     * y emite un JWT. Aplica el control de intentos fallidos por cuenta e IP.
     */
    AuthResponseDTO iniciarSesion(LoginAuthDTO solicitud, String ipCliente);

    /** Emite un JWT para el perfil veterinario demo (revisiones académicas). */
    AuthResponseDTO iniciarSesionVeterinarioDemo(String correoDemo);

    ClienteResponseDTO obtenerPerfil(String correo);

    /**
     * Cierra la sesión del usuario: incrementa su versión de token, con lo que el JWT
     * actual (y cualquier otro emitido antes) deja de ser aceptado por el filtro.
     */
    void cerrarSesion(String correo);
}
