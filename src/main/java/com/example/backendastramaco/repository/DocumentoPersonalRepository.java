package com.example.backendastramaco.repository;

import com.example.backendastramaco.model.DocumentoPersonal;
import com.example.backendastramaco.model.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoPersonalRepository extends JpaRepository<DocumentoPersonal, Long> {
    List<DocumentoPersonal> findByTransportistaId(Long transportistaId);
    boolean existsByTransportistaIdAndTipoDocumento(Long transportistaId, TipoDocumento tipoDocumento);
}