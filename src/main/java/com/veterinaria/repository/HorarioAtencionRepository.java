package com.veterinaria.repository;

import com.veterinaria.model.HorarioAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Long> {
    List<HorarioAtencion> findByVeterinarioIdAndDiaSemanaAndActivoTrue(Long veterinarioId, DayOfWeek diaSemana);

    List<HorarioAtencion> findByVeterinarioIdAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(Long veterinarioId);

    void deleteByVeterinarioId(Long veterinarioId);

    @Query("""
            select count(h) > 0 from HorarioAtencion h
            where h.veterinario.id = :veterinarioId
              and h.diaSemana = :diaSemana
              and h.activo = true
              and h.horaInicio < :horaFin
              and h.horaFin > :horaInicio
            """)
    boolean existeTraslape(
            @Param("veterinarioId") Long veterinarioId,
            @Param("diaSemana") DayOfWeek diaSemana,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);

    @Query("""
            select count(h) > 0 from HorarioAtencion h
            where h.veterinario.id = :veterinarioId
              and h.diaSemana = :diaSemana
              and h.activo = true
              and h.id <> :franjaId
              and h.horaInicio < :horaFin
              and h.horaFin > :horaInicio
            """)
    boolean existeTraslapeExcluyendo(
            @Param("veterinarioId") Long veterinarioId,
            @Param("diaSemana") DayOfWeek diaSemana,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("franjaId") Long franjaId);
}
