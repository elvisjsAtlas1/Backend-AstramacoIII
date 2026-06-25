package com.example.backendastramaco.repository.audit;

import com.example.backendastramaco.model.audit.AuditoriaCarga;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditoriaCargaRepository extends JpaRepository<AuditoriaCarga, Long> {

    Page<AuditoriaCarga> findAllByOrderByFechaHoraDesc(Pageable pageable);

    Page<AuditoriaCarga> findByCargaIdOrderByFechaHoraDesc(Long cargaId, Pageable pageable);

    Page<AuditoriaCarga> findByTransportistaIdOrderByFechaHoraDesc(Long transportistaId, Pageable pageable);

    Page<AuditoriaCarga> findByAccion(String accion, Pageable pageable);

    Page<AuditoriaCarga> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

    long countByAccion(String accion);
}