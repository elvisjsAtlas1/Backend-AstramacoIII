package com.example.backendastramaco.repository.audit;
import com.example.backendastramaco.model.audit.AuditoriaCarga;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditoriaCargaRepository extends JpaRepository<AuditoriaCarga, Long> {
    List<AuditoriaCarga> findByCargaId(Long cargaId);
    List<AuditoriaCarga> findByTransportistaId(Long transportistaId);
}
