package com.example.backendastramaco.model;

import com.example.backendastramaco.model.audit.BaseEntity;
import com.example.backendastramaco.model.enums.TipoDocumento;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "documentos_personales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoPersonal extends BaseEntity {

    //eliminado esta en baseEntity

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDocumento tipoDocumento;


    @Column(nullable = false, length = 50)
    private String valor;

    // 🔥 SOLO PARA SOAT Y REVISION TECNICA
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transportista_id", nullable = false)
    @JsonIgnore
    private Transportista transportista;
}