package com.example.backendastramaco.repository.audit;
import com.example.backendastramaco.model.audit.AuditoriaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditoriaUsuarioRepository extends JpaRepository<AuditoriaUsuario, Long> {
    List<AuditoriaUsuario> findByUsuarioId(Long usuarioId);
    List<AuditoriaUsuario> findByAccion(String accion);
}