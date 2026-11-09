package com.veterinaria.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.veterinaria.exception.DemasiadosIntentosException;

/**
 * Control de fuerza bruta del inicio de sesión (TSK-03-04).
 *
 * <p>Lleva la cuenta de intentos fallidos consecutivos por cuenta (correo) y por IP.
 * Al llegar al máximo configurado (5 por defecto) se bloquean los nuevos intentos de
 * esa cuenta o IP durante el tiempo configurado (15 minutos por defecto) y se
 * responde HTTP 429. Un inicio de sesión exitoso reinicia ambos contadores, y los
 * fallos separados por más que la ventana de bloqueo dejan de considerarse consecutivos.
 *
 * <p>El estado vive en memoria: es suficiente para una sola instancia de la API
 * (el despliegue actual). Con varias instancias habría que moverlo a una base
 * compartida (p. ej. Redis o la tabla usuario).
 */
@Component
public class ControlIntentosLoginService {

    private static final int LIMITE_ENTRADAS_ANTES_DE_LIMPIAR = 10_000;

    private record Estado(int fallos, Instant ultimoFallo, Instant bloqueadoHasta) {
    }

    private final Map<String, Estado> estados = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxIntentos;
    private final Duration duracionBloqueo;

    public ControlIntentosLoginService(Clock clock,
            @Value("${app.seguridad.login.max-intentos:5}") int maxIntentos,
            @Value("${app.seguridad.login.bloqueo-minutos:15}") long bloqueoMinutos) {
        if (maxIntentos < 1 || bloqueoMinutos < 1) {
            throw new IllegalArgumentException("La política de bloqueo de inicio de sesión no es válida");
        }
        this.clock = clock;
        this.maxIntentos = maxIntentos;
        this.duracionBloqueo = Duration.ofMinutes(bloqueoMinutos);
    }

    /** Lanza {@link DemasiadosIntentosException} si la cuenta o la IP están bloqueadas. */
    public void verificarPermitido(String correo, String ip) {
        Instant ahora = clock.instant();
        long segundos = Math.max(
                segundosRestantes(claveCuenta(correo), ahora),
                segundosRestantes(claveIp(ip), ahora));
        if (segundos > 0) {
            throw new DemasiadosIntentosException(segundos);
        }
    }

    public void registrarFallo(String correo, String ip) {
        Instant ahora = clock.instant();
        if (estados.size() > LIMITE_ENTRADAS_ANTES_DE_LIMPIAR) {
            limpiarVencidos(ahora);
        }
        registrarFallo(claveCuenta(correo), ahora);
        registrarFallo(claveIp(ip), ahora);
    }

    public void registrarExito(String correo, String ip) {
        String cuenta = claveCuenta(correo);
        String direccion = claveIp(ip);
        if (cuenta != null) {
            estados.remove(cuenta);
        }
        if (direccion != null) {
            estados.remove(direccion);
        }
    }

    /** Borra todos los contadores. Pensado para pruebas y operación manual. */
    public void reiniciar() {
        estados.clear();
    }

    private void registrarFallo(String clave, Instant ahora) {
        if (clave == null) {
            return;
        }
        estados.compute(clave, (k, actual) -> {
            int fallos = esConsecutivo(actual, ahora) ? actual.fallos() + 1 : 1;
            Instant bloqueadoHasta = fallos >= maxIntentos ? ahora.plus(duracionBloqueo) : null;
            return new Estado(fallos, ahora, bloqueadoHasta);
        });
    }

    private boolean esConsecutivo(Estado actual, Instant ahora) {
        if (actual == null) {
            return false;
        }
        if (actual.bloqueadoHasta() != null) {
            // Si el bloqueo ya venció, el conteo empieza de nuevo.
            return actual.bloqueadoHasta().isAfter(ahora);
        }
        return actual.ultimoFallo().plus(duracionBloqueo).isAfter(ahora);
    }

    private long segundosRestantes(String clave, Instant ahora) {
        if (clave == null) {
            return 0;
        }
        Estado estado = estados.get(clave);
        if (estado == null || estado.bloqueadoHasta() == null) {
            return 0;
        }
        if (!estado.bloqueadoHasta().isAfter(ahora)) {
            estados.remove(clave, estado);
            return 0;
        }
        long milisegundos = Duration.between(ahora, estado.bloqueadoHasta()).toMillis();
        return (milisegundos + 999) / 1000;
    }

    private void limpiarVencidos(Instant ahora) {
        estados.entrySet().removeIf(entrada -> {
            Estado estado = entrada.getValue();
            Instant vigenteHasta = estado.bloqueadoHasta() != null
                    ? estado.bloqueadoHasta()
                    : estado.ultimoFallo().plus(duracionBloqueo);
            return !vigenteHasta.isAfter(ahora);
        });
    }

    private String claveCuenta(String correo) {
        return correo == null || correo.isBlank() ? null : "cuenta:" + correo.trim().toLowerCase(Locale.ROOT);
    }

    private String claveIp(String ip) {
        return ip == null || ip.isBlank() ? null : "ip:" + ip.trim();
    }
}
