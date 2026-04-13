package com.example.backendastramaco.repository;

import com.example.backendastramaco.model.Pedido;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByTransportistaIdOrderByHoraEnvioDesc(Long transportistaId);
}