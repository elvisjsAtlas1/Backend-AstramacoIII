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
    private String accion;            // CREATE, UPDATE, DELETE, RESTORE, DELETE_PERMANENT, UPDATE_ESTADO
    private String username;

    // Campos específicos para tracking
    private String clienteNombreAnterior;
    private String clienteNombreNuevo;
    private String clienteTelefonoAnterior;
    private String clienteTelefonoNuevo;
    private String direccionEnvioAnterior;
    private String direccionEnvioNuevo;
    private String tipoTransporteAnterior;
    private String tipoTransporteNuevo;
    private String materialAnterior;
    private String materialNuevo;
    private Double cantidadAnterior;
    private Double cantidadNuevo;
    private Double montoTotalAnterior;
    private Double montoTotalNuevo;
    private Double adelantoAnterior;
    private Double adelantoNuevo;
    private String estadoAnterior;
    private String estadoNuevo;

    private LocalDateTime fechaHora;
    private String ipAddress;

    @Column(length = 5000)
    private String datosCompletosAnteriores;
    @Column(length = 5000)
    private String datosCompletosNuevos;
}