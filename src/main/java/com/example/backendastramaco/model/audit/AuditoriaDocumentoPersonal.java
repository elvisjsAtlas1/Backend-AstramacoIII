package com.example.backendastramaco.model.audit;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_documentos")
@Data
public class AuditoriaDocumentoPersonal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long documentoId;
    private Long transportistaId;
    private String accion;
    private String username;

    private String tipoDocumentoAnterior;
    private String tipoDocumentoNuevo;
    private String valorAnterior;
    private String valorNuevo;
    private LocalDateTime fechaVencimientoAnterior;
    private LocalDateTime fechaVencimientoNuevo;

    private LocalDateTime fechaHora;
    private String ipAddress;

    @Column(length = 5000)
    private String datosCompletosAnteriores;
    @Column(length = 5000)
    private String datosCompletosNuevos;
}