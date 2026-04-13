package com.example.backendastramaco.model;

import com.example.backendastramaco.model.enums.TipoMaterial;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "cargas",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"transportista_id", "tipo_material"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transportista_id", nullable = false)
    private Transportista transportista;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_material", nullable = false)
    private TipoMaterial tipoMaterial;

    @Column(nullable = false)
    private Double cantidadDisponible;
}