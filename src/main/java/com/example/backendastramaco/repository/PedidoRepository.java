package com.example.backendastramaco.repository;

import com.example.backendastramaco.model.Pedido;
import com.example.backendastramaco.model.enums.EstadoPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Soft delete queries
    Page<Pedido> findByDeletedAtIsNull(Pageable pageable);
    Page<Pedido> findByDeletedAtIsNotNull(Pageable pageable);

    // Filtros con soft delete
    Page<Pedido> findByEstadoAndDeletedAtIsNull(EstadoPedido estado, Pageable pageable);
    Page<Pedido> findByTransportistaIdAndDeletedAtIsNull(Long transportistaId, Pageable pageable);
    Page<Pedido> findByEstadoAndTransportistaIdAndDeletedAtIsNull(EstadoPedido estado, Long transportistaId, Pageable pageable);

    // Listas sin paginación para compatibilidad
    List<Pedido> findByTransportistaIdAndDeletedAtIsNullOrderByHoraEnvioDesc(Long transportistaId);

    // Búsqueda por transportista con estado
    Page<Pedido> findByTransportistaIdAndEstadoAndDeletedAtIsNull(Long transportistaId, EstadoPedido estado, Pageable pageable);
}