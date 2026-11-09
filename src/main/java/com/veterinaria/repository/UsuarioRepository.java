package com.veterinaria.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veterinaria.model.Rol;
import com.veterinaria.model.Usuario;

import jakarta.persistence.LockModeType;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByClienteId(Long clienteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdForUpdate(@Param("id") Long id);

    @Query("select u.id from Usuario u where u.activo = true and :rol member of u.roles order by u.id")
    List<Long> findIdsActivosConRol(@Param("rol") Rol rol);

    @Query("select count(u) from Usuario u where u.activo = true and :rol member of u.roles")
    long contarActivosConRol(@Param("rol") Rol rol);
}
