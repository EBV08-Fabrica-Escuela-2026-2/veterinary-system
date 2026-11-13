package com.veterinaria.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.veterinaria.dto.CitaReporteFila;
import com.veterinaria.model.Cita;

public interface ReporteCitasRepository extends Repository<Cita, Long> {

    @Query("""
            select new com.veterinaria.dto.CitaReporteFila(s.nombre, c.estado, s.precio, c.fechaHoraInicio)
            from Cita c join c.servicio s
            where c.fechaHoraInicio >= :desde and c.fechaHoraInicio < :hasta
            """)
    List<CitaReporteFila> filasDelPeriodo(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("select distinct s.nombre from Servicio s order by s.nombre")
    List<String> nombresServicios();
}