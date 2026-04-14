package com.example.backendastramaco.dto;

import com.example.backendastramaco.model.enums.TipoTransporte;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*;

@Getter
@Setter
public class TransportistaRequestDTO {

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellidos;

    @NotBlank
    @Size(min = 8, max = 8)
    private String dni;

    @Min(18)
    private int edad;

    @NotNull
    private TipoTransporte tipoTransporte;

    @NotBlank
    private String placa;

    private String vehiculoInfo;

    @NotNull
    @Positive
    private Double capacidad;

    private String estado;

    @NotNull
    private Long usuarioId;
}