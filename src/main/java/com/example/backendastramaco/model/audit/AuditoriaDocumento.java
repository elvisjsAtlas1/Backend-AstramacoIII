package com.example.backendastramaco.model.audit;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_documentos")
@Data
public class AuditoriaDocumento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long documentoId;
    private Long transportistaId;
    private String accion;
    private String username;

    // Campos específicos para tracking
    private String tipoDocumentoAnterior;
    private String tipoDocumentoNuevo;
    private String valorAnterior;
    private String valorNuevo;
    private LocalDate fechaEmisionAnterior;
    private LocalDate fechaEmisionNuevo;
    private LocalDate fechaVencimientoAnterior;
    private LocalDate fechaVencimientoNuevo;
    private Boolean activoAnterior;
    private Boolean activoNuevo;

    private LocalDateTime fechaHora;
    private String ipAddress;

    @Column(length = 5000)
    private String datosCompletosAnteriores;
    @Column(length = 5000)
    private String datosCompletosNuevos;
}