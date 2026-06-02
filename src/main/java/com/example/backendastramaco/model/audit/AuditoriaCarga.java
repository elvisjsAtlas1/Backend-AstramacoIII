package com.example.backendastramaco.model.audit;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_cargas")
@Data
public class AuditoriaCarga {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long cargaId;
    private Long transportistaId;
    private String accion;
    private String username;

    private String tipoTarjetaAnterior;
    private String tipoTarjetaNuevo;
    private Integer cantidadDisponibleAnterior;
    private Integer cantidadDisponibleNuevo;
    private String estadoAnterior;
    private String estadoNuevo;

    private LocalDateTime fechaHora;
    private String ipAddress;

    @Column(length = 5000)
    private String datosCompletosAnteriores;
    @Column(length = 5000)
    private String datosCompletosNuevos;
}