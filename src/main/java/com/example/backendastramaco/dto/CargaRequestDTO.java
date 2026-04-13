package com.example.backendastramaco.dto;

import com.example.backendastramaco.model.enums.TipoMaterial;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CargaRequestDTO {

    @NotNull(message = "El tipo de material es obligatorio")
    private TipoMaterial tipoMaterial;

    @NotNull(message = "La cantidad disponible es obligatoria")
    @DecimalMin(value = "0.0", inclusive = true, message = "La cantidad no puede ser negativa")
    private Double cantidadDisponible;
}