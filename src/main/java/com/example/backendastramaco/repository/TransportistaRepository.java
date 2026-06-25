package com.example.backendastramaco.repository;

import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.enums.EstadoTransportista;
import com.example.backendastramaco.model.enums.TipoTransporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransportistaRepository extends JpaRepository<Transportista, Long> {

    // Búsquedas básicas
    Optional<Transportista> findByDni(String dni);
    Optional<Transportista> findByUsuario(Usuario usuario);
    Optional<Transportista> findByPlaca(String placa);

    // Validaciones de unicidad
    boolean existsByDni(String dni);
    boolean existsByPlaca(String placa);
    boolean existsByUsuario(Usuario usuario);

    // Consultas con soft delete
    Page<Transportista> findByDeletedAtIsNull(Pageable pageable);
    Page<Transportista> findByDeletedAtIsNotNull(Pageable pageable);

    // Filtros con soft delete
    Page<Transportista> findByTipoTransporteAndDeletedAtIsNull(TipoTransporte tipoTransporte, Pageable pageable);
    Page<Transportista> findByEstadoAndDeletedAtIsNull(EstadoTransportista estado, Pageable pageable);
    Page<Transportista> findByTipoTransporteAndEstadoAndDeletedAtIsNull(TipoTransporte tipoTransporte, EstadoTransportista estado, Pageable pageable);

    // Búsquedas específicas
    List<Transportista> findByTipoTransporteAndEstadoAndDeletedAtIsNull(TipoTransporte tipoTransporte, EstadoTransportista estado);
    Optional<Transportista> findByDniAndDeletedAtIsNull(String dni);
    Optional<Transportista> findByUsuarioAndDeletedAtIsNull(Usuario usuario);
}