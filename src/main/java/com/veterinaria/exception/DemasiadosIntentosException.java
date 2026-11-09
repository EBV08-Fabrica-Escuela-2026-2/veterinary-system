package com.veterinaria.exception;

/** Inicio de sesión bloqueado temporalmente por intentos fallidos (HTTP 429). */
public class DemasiadosIntentosException extends RuntimeException {

    private final long segundosRestantes;

    public DemasiadosIntentosException(long segundosRestantes) {
        super(construirMensaje(segundosRestantes));
        this.segundosRestantes = segundosRestantes;
    }

    public long getSegundosRestantes() {
        return segundosRestantes;
    }

    private static String construirMensaje(long segundosRestantes) {
        long minutos = Math.max(1, (segundosRestantes + 59) / 60);
        return "Demasiados intentos fallidos. Intenta nuevamente en " + minutos
                + (minutos == 1 ? " minuto." : " minutos.");
    }
}
