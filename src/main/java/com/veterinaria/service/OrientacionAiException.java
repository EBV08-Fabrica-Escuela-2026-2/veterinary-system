package com.veterinaria.service;

public class OrientacionAiException extends RuntimeException {

    public OrientacionAiException(String message) {
        super(message);
    }

    public OrientacionAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
