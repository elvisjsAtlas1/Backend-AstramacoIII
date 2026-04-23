package com.example.backendastramaco.dto;

import com.example.backendastramaco.model.enums.Rol;
import lombok.Data;

@Data
public class UsuarioRequestDTO {

    private String username;
    private String password;
    private Rol rol;
}