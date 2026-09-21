package com.veterinaria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veterinaria.model.Veterinario;

@Repository
public interface VeterinarioRepository extends JpaRepository<Veterinario, Long> {
    List<Veterinario> findByActivoTrue();

    boolean existsByDocumentoIdentidad(String documentoIdentidad);

    boolean existsByCorreo(String correo);
}
