package com.example.backendastramaco.model.audit;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_pedidos")
@Data
public class AuditoriaPedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pedidoId;
    private String accion;            // CREATE, UPDATE, DELETE
    private String username;          // Usuario que realizó la acción

    private Long transportistaIdAnterior;
    private Long transportistaIdNuevo;
    private String tipoTarjetaAnterior;
    private String tipoTarjetaNuevo;
    private Integer cantidadAnterior;
    private Integer cantidadNuevo;
    private String estadoAnterior;
    private String estadoNuevo;
    private LocalDateTime fechaEntregaAnterior;
    private LocalDateTime fechaEntregaNuevo;

    private LocalDateTime fechaHora;
    private String ipAddress;

    @Column(length = 5000)
    private String datosCompletosAnteriores;
    @Column(length = 5000)
    private String datosCompletosNuevos;
}