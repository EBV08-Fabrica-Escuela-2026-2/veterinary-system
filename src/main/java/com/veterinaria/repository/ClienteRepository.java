package com.veterinaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veterinaria.model.Cliente;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByDocumentoIdentidad(String documentoIdentidad);

    boolean existsByCorreo(String correo);

    Optional<Cliente> findByDocumentoIdentidad(String documentoIdentidad);

    Optional<Cliente> findByCorreo(String correo);
}
