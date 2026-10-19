package com.veterinaria.service;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.veterinaria.exception.FranjaTraslapadaException;
import com.veterinaria.model.FranjaHoraria;

@Component
public class ValidadorFranjasHorarias {

    public static final String MENSAJE_TRASLAPE = "Las franjas de un mismo día no pueden traslaparse";

    public boolean seTraslapa(LocalTime inicio1, LocalTime fin1, LocalTime inicio2, LocalTime fin2) {
        return inicio1.isBefore(fin2) && inicio2.isBefore(fin1);
    }

    public void validarRango(LocalTime inicio, LocalTime fin) {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new IllegalArgumentException("Cada franja debe tener una hora final posterior a la inicial");
        }
    }

    public void validarSinTraslapes(List<FranjaHoraria> franjasDelDia) {
        franjasDelDia.forEach(franja -> validarRango(franja.horaInicio(), franja.horaFin()));

        List<FranjaHoraria> ordenadas = franjasDelDia.stream()
                .sorted(Comparator.comparing(FranjaHoraria::horaInicio))
                .toList();
        for (int i = 1; i < ordenadas.size(); i++) {
            FranjaHoraria anterior = ordenadas.get(i - 1);
            FranjaHoraria actual = ordenadas.get(i);
            if (seTraslapa(anterior.horaInicio(), anterior.horaFin(), actual.horaInicio(), actual.horaFin())) {
                throw new FranjaTraslapadaException(MENSAJE_TRASLAPE);
            }
        }
    }
}
