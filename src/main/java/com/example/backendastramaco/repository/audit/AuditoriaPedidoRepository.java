package com.example.backendastramaco.repository.audit;
import com.example.backendastramaco.model.audit.AuditoriaPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditoriaPedidoRepository extends JpaRepository<AuditoriaPedido, Long> {
    List<AuditoriaPedido> findByPedidoId(Long pedidoId);
}