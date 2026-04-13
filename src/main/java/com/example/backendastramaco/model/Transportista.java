package com.example.backendastramaco.model;

import com.example.backendastramaco.model.enums.EstadoTransportista;
import com.example.backendastramaco.model.enums.TipoTransporte;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "transportistas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transportista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    private String nombre;
    private String apellidos;
    private String dni;
    private Integer edad;

    @Enumerated(EnumType.STRING)
    private TipoTransporte tipoTransporte;

    private String placa;
    private String vehiculoInfo;

    private Double capacidad;

    @OneToMany(mappedBy = "transportista", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentoPersonal> documentos;

    @Enumerated(EnumType.STRING)
    private EstadoTransportista estado;


}