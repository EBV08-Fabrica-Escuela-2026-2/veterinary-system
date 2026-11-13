package com.veterinaria.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veterinaria.model.BloqueoHorario;

@Repository
public interface BloqueoHorarioRepository extends JpaRepository<BloqueoHorario, Long> {

    List<BloqueoHorario> findByVeterinarioIdAndActivoTrueAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
            Long veterinarioId, LocalDateTime hasta, LocalDateTime desde);

    List<BloqueoHorario> findByVeterinarioIdAndActivoTrue(Long veterinarioId);
}