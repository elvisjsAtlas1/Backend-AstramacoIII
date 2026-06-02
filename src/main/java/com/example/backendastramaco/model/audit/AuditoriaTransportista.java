package com.example.backendastramaco.model.audit;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_transportistas")
@Data
public class AuditoriaTransportista {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long transportistaId;
    private String accion;            // CREATE, UPDATE, DELETE
    private String username;          // Usuario que realizó la acción

    // Campos específicos para tracking
    private String nombreAnterior;
    private String nombreNuevo;
    private String apellidosAnterior;
    private String apellidosNuevo;
    private String dniAnterior;
    private String dniNuevo;
    private Integer edadAnterior;
    private Integer edadNuevo;
    private String tipoTransporteAnterior;
    private String tipoTransporteNuevo;
    private String placaAnterior;
    private String placaNuevo;
    private String vehiculoInfoAnterior;
    private String vehiculoInfoNuevo;
    private Double capacidadAnterior;
    private Double capacidadNuevo;
    private String estadoAnterior;
    private String estadoNuevo;

    private LocalDateTime fechaHora;
    private String ipAddress;

    @Column(length = 5000)
    private String datosCompletosAnteriores; // JSON completo
    @Column(length = 5000)
    private String datosCompletosNuevos;
}