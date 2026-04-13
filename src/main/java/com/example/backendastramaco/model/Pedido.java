package com.example.backendastramaco.model;

import com.example.backendastramaco.model.enums.EstadoPedido;
import com.example.backendastramaco.model.enums.TipoMaterial;
import com.example.backendastramaco.model.enums.TipoTransporte;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clienteNombre;
    private String clienteTelefono;
    private String direccionEnvio;

    @Enumerated(EnumType.STRING)
    private TipoTransporte tipoTransporte;

    @Enumerated(EnumType.STRING)
    private TipoMaterial material;

    private Double cantidad;
    private Double montoTotal;
    private Double adelanto;

    private Integer piso;
    private LocalDateTime horaEnvio;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoPedido estado = EstadoPedido.EN_ENVIO;

    private String codigoVerificacion;

    @ManyToOne
    @JoinColumn(name = "transportista_id")
    private Transportista transportista;
}