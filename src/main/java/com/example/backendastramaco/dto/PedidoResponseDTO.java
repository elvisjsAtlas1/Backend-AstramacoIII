package com.example.backendastramaco.dto;

import com.example.backendastramaco.model.enums.EstadoPedido;
import com.example.backendastramaco.model.enums.TipoMaterial;
import com.example.backendastramaco.model.enums.TipoTransporte;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PedidoResponseDTO {

    private Long id;

    private String clienteNombre;
    private String clienteTelefono;
    private String direccionEnvio;

    private TipoTransporte tipoTransporte;
    private TipoMaterial material;

    private Double cantidad;
    private Double montoTotal;
    private Double adelanto;

    private Integer piso;
    private LocalDateTime horaEnvio;

    private Long transportistaId;
    private String transportistaNombre;

    private EstadoPedido estado;
    private String codigoVerificacion;
}