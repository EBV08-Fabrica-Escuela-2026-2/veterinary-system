package com.veterinaria.service;

import java.util.List;

public interface OrientacionAiClient {

    Resultado analizar(
            String especie,
            int edad,
            String sintomas,
            int duracionValor,
            String duracionUnidad,
            List<ServicioCandidato> servicios);

    record ServicioCandidato(Long id, String nombre, String descripcion) {
    }

    record ServicioSeleccionado(Long servicioId, String explicacion) {
    }

    record Resultado(boolean urgente, List<ServicioSeleccionado> recomendaciones) {
    }
}
