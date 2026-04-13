package com.example.backendastramaco.dto;

import com.example.backendastramaco.model.enums.TipoMaterial;
import com.example.backendastramaco.model.enums.TipoTransporte;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PedidoRequestDTO {

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
}