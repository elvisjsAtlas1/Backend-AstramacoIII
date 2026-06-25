package com.example.backendastramaco.repository;

import com.example.backendastramaco.model.DocumentoPersonal;
import com.example.backendastramaco.model.enums.TipoDocumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoPersonalRepository extends JpaRepository<DocumentoPersonal, Long> {

    // Soft delete queries
    Page<DocumentoPersonal> findByDeletedAtIsNull(Pageable pageable);
    Page<DocumentoPersonal> findByDeletedAtIsNotNull(Pageable pageable);

    // Búsquedas por transportista con soft delete
    List<DocumentoPersonal> findByTransportistaIdAndDeletedAtIsNull(Long transportistaId);
    Page<DocumentoPersonal> findByTransportistaIdAndDeletedAtIsNull(Long transportistaId, Pageable pageable);
    List<DocumentoPersonal> findByTransportistaIdAndActivoTrueAndDeletedAtIsNull(Long transportistaId);

    // Validación de unicidad
    boolean existsByTransportistaIdAndTipoDocumentoAndDeletedAtIsNull(Long transportistaId, TipoDocumento tipoDocumento);

    // Búsqueda por ID y transportista
    Optional<DocumentoPersonal> findByIdAndTransportistaIdAndDeletedAtIsNull(Long id, Long transportistaId);
}