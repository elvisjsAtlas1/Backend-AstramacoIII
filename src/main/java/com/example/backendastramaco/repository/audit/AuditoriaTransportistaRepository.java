package com.example.backendastramaco.repository.audit;
import com.example.backendastramaco.model.audit.AuditoriaTransportista;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditoriaTransportistaRepository extends JpaRepository<AuditoriaTransportista, Long> {
    List<AuditoriaTransportista> findAllByOrderByFechaHoraDesc();
    List<AuditoriaTransportista> findByTransportistaIdOrderByFechaHoraDesc(Long transportistaId);
}