package com.veterinaria.exception;

public class ClienteNoAutenticadoException extends RuntimeException {

    public ClienteNoAutenticadoException(String message) {
        super(message);
    }
}
