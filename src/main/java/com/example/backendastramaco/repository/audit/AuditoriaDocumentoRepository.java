package com.example.backendastramaco.repository.audit;
import com.example.backendastramaco.model.audit.AuditoriaDocumentoPersonal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditoriaDocumentoRepository extends JpaRepository<AuditoriaDocumentoPersonal, Long> {
    List<AuditoriaDocumentoPersonal> findByDocumentoId(Long documentoId);
    List<AuditoriaDocumentoPersonal> findByTransportistaId(Long transportistaId);
}