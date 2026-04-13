package com.example.backendastramaco.dto;

import com.example.backendastramaco.model.enums.TipoTransporte;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransportistaRequestDTO {

    public String nombre;
    public String apellidos;
    public String dni;
    public int edad;
    public TipoTransporte tipoTransporte;
    public String placa;
    public String vehiculoInfo;
    public Double capacidad;
    public String estado; // 👈 aquí está la clave
    public Long usuarioId;
}