package com.example.backendastramaco.dto;

import com.example.backendastramaco.model.enums.TipoTransporte;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TransportistaRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    @NotBlank(message = "El DNI es obligatorio")
    @Size(min = 8, max = 8, message = "El DNI debe tener 8 dígitos")
    private String dni;

    @Min(value = 18, message = "La edad mínima es 18 años")
    private int edad;

    @NotNull(message = "El tipo de transporte es obligatorio")
    private TipoTransporte tipoTransporte;

    @NotBlank(message = "La placa es obligatoria")
    private String placa;

    private String vehiculoInfo;

    @DecimalMin(value = "0.0", inclusive = true, message = "La capacidad no puede ser negativa")
    private Double capacidad;

    private String estado;


}