package com.veterinaria.exception;

/**
 * El cambio dejaría a la clínica sin administrador activo o es una auto-desactivación
 * no permitida (HTTP 409).
 */
public class IntegridadAdministrativaException extends RuntimeException {

    public IntegridadAdministrativaException(String message) {
        super(message);
    }
}
