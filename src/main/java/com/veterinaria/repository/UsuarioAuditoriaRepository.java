package com.veterinaria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veterinaria.model.UsuarioAuditoria;

@Repository
public interface UsuarioAuditoriaRepository extends JpaRepository<UsuarioAuditoria, Long> {

    List<UsuarioAuditoria> findByUsuarioIdOrderByFechaDescIdDesc(Long usuarioId);
}
