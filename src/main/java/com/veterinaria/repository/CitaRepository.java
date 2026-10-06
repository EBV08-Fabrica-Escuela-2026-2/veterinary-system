package com.veterinaria.repository;

import com.veterinaria.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {
    List<Cita> findByVeterinarioIdAndEstadoAndFechaHoraInicioGreaterThanEqualAndFechaHoraInicioLessThan(
            Long veterinarioId, String estado, LocalDateTime desde, LocalDateTime hasta);

    List<Cita> findByClienteIdOrderByFechaHoraInicioDesc(Long clienteId);
}