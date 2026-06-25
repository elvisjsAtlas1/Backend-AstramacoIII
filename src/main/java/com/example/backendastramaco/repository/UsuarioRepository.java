package com.example.backendastramaco.repository;

import com.example.backendastramaco.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    // Para usuarios no eliminados
    Page<Usuario> findByDeletedAtIsNull(Pageable pageable);

    Page<Usuario> findByDeletedAtIsNotNull(Pageable pageable);

    Page<Usuario> findByRolAndDeletedAtIsNull(String rol, Pageable pageable);

    Page<Usuario> findByActivoAndDeletedAtIsNull(Boolean activo, Pageable pageable);

    Page<Usuario> findByRolAndActivoAndDeletedAtIsNull(String rol, Boolean activo, Pageable pageable);
}