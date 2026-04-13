package com.example.backendastramaco.repository;

import com.example.backendastramaco.model.Transportista;

import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.enums.EstadoTransportista;
import com.example.backendastramaco.model.enums.TipoTransporte;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;

public interface TransportistaRepository extends JpaRepository<Transportista, Long> {
    List<Transportista> findByTipoTransporteAndEstado(
            TipoTransporte tipo, EstadoTransportista estado);
    List<Transportista> findByTipoTransporte(TipoTransporte tipoTransporte);
    Optional<Transportista> findByUsuario(Usuario usuario);

}