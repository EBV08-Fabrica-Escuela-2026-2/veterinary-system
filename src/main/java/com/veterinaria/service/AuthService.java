package com.veterinaria.service;

import com.veterinaria.dto.AuthResponseDTO;
import com.veterinaria.dto.ClienteResponseDTO;
import com.veterinaria.dto.LoginAuthDTO;
import com.veterinaria.dto.RegistroAuthDTO;

public interface AuthService {

    AuthResponseDTO registrar(RegistroAuthDTO solicitud);

    AuthResponseDTO iniciarSesion(LoginAuthDTO solicitud);

    ClienteResponseDTO obtenerPerfil(String correo);
}
