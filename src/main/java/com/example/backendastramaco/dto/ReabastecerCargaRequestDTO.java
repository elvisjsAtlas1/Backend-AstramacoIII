package com.example.backendastramaco.dto;

import com.example.backendastramaco.model.enums.TipoMaterial;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReabastecerCargaRequestDTO {

    @NotNull(message = "El tipo de material es obligatorio")
    private TipoMaterial tipoMaterial;

    @NotNull(message = "La cantidad a agregar es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad a agregar debe ser mayor a cero")
    private Double cantidadAgregar;
}