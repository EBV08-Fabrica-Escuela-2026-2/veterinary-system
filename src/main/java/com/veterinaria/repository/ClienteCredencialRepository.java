package com.veterinaria.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veterinaria.model.ClienteCredencial;

@Repository
public interface ClienteCredencialRepository extends JpaRepository<ClienteCredencial, Long> {

    Optional<ClienteCredencial> findByClienteId(Long clienteId);

    boolean existsByClienteId(Long clienteId);
}