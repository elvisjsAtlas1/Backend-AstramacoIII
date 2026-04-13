package com.example.backendastramaco.dto;

import com.example.backendastramaco.model.enums.TipoMaterial;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CargaResponseDTO {

    private Long id;
    private Long transportistaId;
    private String transportistaNombre;
    private TipoMaterial tipoMaterial;
    private Double cantidadDisponible;
}