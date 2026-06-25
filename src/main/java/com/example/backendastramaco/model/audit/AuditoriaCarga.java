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

    // Campos específicos para tracking
    private String tipoMaterialAnterior;
    private String tipoMaterialNuevo;
    private Double cantidadAnterior;
    private Double cantidadNuevo;

    private LocalDateTime fechaHora;
    private String ipAddress;

    @Column(length = 5000)
    private String datosCompletosAnteriores;
    @Column(length = 5000)
    private String datosCompletosNuevos;
}