package com.example.backendastramaco.repository.audit;

import com.example.backendastramaco.model.audit.AuditoriaUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditoriaUsuarioRepository extends JpaRepository<AuditoriaUsuario, Long> {

    Page<AuditoriaUsuario> findAllByOrderByFechaHoraDesc(Pageable pageable);

    Page<AuditoriaUsuario> findByUsuarioIdOrderByFechaHoraDesc(Long usuarioId, Pageable pageable);

    Page<AuditoriaUsuario> findByAccion(String accion, Pageable pageable);

    Page<AuditoriaUsuario> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

    long countByAccion(String accion);
}