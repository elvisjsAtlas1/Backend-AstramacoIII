package com.example.backendastramaco.repository;

import com.example.backendastramaco.model.Carga;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface CargaRepository extends JpaRepository<Carga, Long> {

    Optional<Carga> findByTransportistaId(Long transportistaId);
}