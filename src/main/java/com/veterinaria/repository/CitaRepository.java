package com.veterinaria.repository;

import com.veterinaria.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {
    List<Cita> findByVeterinarioIdAndEstadoAndFechaHoraInicioGreaterThanEqualAndFechaHoraInicioLessThan(
            Long veterinarioId, String estado, LocalDateTime desde, LocalDateTime hasta);

        List<Cita> findByVeterinarioIdAndEstadoAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
            Long veterinarioId, String estado, LocalDateTime hasta, LocalDateTime desde);

    List<Cita> findByClienteIdOrderByFechaHoraInicioDesc(Long clienteId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select c from Cita c where c.id = :id")
        Optional<Cita> findByIdForUpdate(@Param("id") Long id);
}