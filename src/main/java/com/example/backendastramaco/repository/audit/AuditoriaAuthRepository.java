package com.example.backendastramaco.repository.audit;
import com.example.backendastramaco.model.audit.AuditoriaAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditoriaAuthRepository extends JpaRepository<AuditoriaAuth, Long> {
    List<AuditoriaAuth> findByUsername(String username);
    List<AuditoriaAuth> findByAccion(String accion);
}