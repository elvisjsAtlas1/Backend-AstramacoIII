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

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false) // SonarQube: Especificar nombre de columna explícito
    private TipoDocumento tipoDocumento;

    @Column(nullable = false, length = 50)
    private String valor;

    // SonarQube: Asegura el mapeo correcto con la columna exacta de phpMyAdmin
    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transportista_id", nullable = false)
    @JsonIgnore
    private Transportista transportista;

    // 🔥 REGLA DE NEGOCIO PARA MANTENIBILIDAD (SonarQube & DB Safety)
    @PrePersist
    @PreUpdate
    private void validarReglasDeFechas() {
        if (this.tipoDocumento == TipoDocumento.SOAT || this.tipoDocumento == TipoDocumento.REVISION_TECNICA) {
            // Validamos que para estos dos tipos NO sean nulas las fechas al registrar o actualizar
            if (this.fechaEmision == null || this.fechaVencimiento == null) {
                throw new IllegalStateException("Para SOAT y REVISION_TECNICA es obligatorio registrar fecha de emision y vencimiento.");
            }
        } else {
            // Forzamos a que si es LICENCIA o TARJETA_CIRCULACION, las fechas sean NULL en la base de datos de forma estricta
            this.fechaEmision = null;
            this.fechaVencimiento = null;
        }
    }
}