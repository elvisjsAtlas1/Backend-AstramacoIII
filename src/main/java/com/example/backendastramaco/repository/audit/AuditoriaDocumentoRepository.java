package com.example.backendastramaco.repository.audit;

import com.example.backendastramaco.model.audit.AuditoriaDocumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditoriaDocumentoRepository extends JpaRepository<AuditoriaDocumento, Long> {

    Page<AuditoriaDocumento> findAllByOrderByFechaHoraDesc(Pageable pageable);

    Page<AuditoriaDocumento> findByDocumentoIdOrderByFechaHoraDesc(Long documentoId, Pageable pageable);

    Page<AuditoriaDocumento> findByTransportistaIdOrderByFechaHoraDesc(Long transportistaId, Pageable pageable);

    Page<AuditoriaDocumento> findByAccion(String accion, Pageable pageable);

    Page<AuditoriaDocumento> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

    long countByAccion(String accion);
}