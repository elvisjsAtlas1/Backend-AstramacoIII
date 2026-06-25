package com.example.backendastramaco.repository.audit;

import com.example.backendastramaco.model.audit.AuditoriaTransportista;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditoriaTransportistaRepository extends JpaRepository<AuditoriaTransportista, Long> {

    Page<AuditoriaTransportista> findAllByOrderByFechaHoraDesc(Pageable pageable);

    Page<AuditoriaTransportista> findByTransportistaIdOrderByFechaHoraDesc(Long transportistaId, Pageable pageable);

    Page<AuditoriaTransportista> findByAccion(String accion, Pageable pageable);

    Page<AuditoriaTransportista> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

    long countByAccion(String accion);
}