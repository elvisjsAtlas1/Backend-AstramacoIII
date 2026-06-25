package com.example.backendastramaco.repository;

import com.example.backendastramaco.model.Carga;
import com.example.backendastramaco.model.enums.TipoMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CargaRepository extends JpaRepository<Carga, Long> {

    // ✅ Método que necesita PedidoService (sin soft delete)
    Optional<Carga> findByTransportistaId(Long transportistaId);

    // ✅ Métodos con soft delete
    Optional<Carga> findByTransportistaIdAndDeletedAtIsNull(Long transportistaId);

    Page<Carga> findByDeletedAtIsNull(Pageable pageable);
    Page<Carga> findByDeletedAtIsNotNull(Pageable pageable);

    Page<Carga> findByTransportistaIdAndDeletedAtIsNull(Long transportistaId, Pageable pageable);

    Optional<Carga> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByTransportistaIdAndDeletedAtIsNull(Long transportistaId);

    Page<Carga> findByTipoMaterialAndDeletedAtIsNull(TipoMaterial tipoMaterial, Pageable pageable);
}