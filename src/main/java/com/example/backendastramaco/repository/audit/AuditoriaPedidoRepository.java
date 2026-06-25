package com.example.backendastramaco.repository.audit;

import com.example.backendastramaco.model.audit.AuditoriaPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditoriaPedidoRepository extends JpaRepository<AuditoriaPedido, Long> {

    Page<AuditoriaPedido> findAllByOrderByFechaHoraDesc(Pageable pageable);

    Page<AuditoriaPedido> findByPedidoIdOrderByFechaHoraDesc(Long pedidoId, Pageable pageable);

    Page<AuditoriaPedido> findByAccion(String accion, Pageable pageable);

    Page<AuditoriaPedido> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

    long countByAccion(String accion);
}