package com.example.backendastramaco.controller;

import com.example.backendastramaco.dto.UsuarioRequestDTO;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public Usuario crear(@RequestBody UsuarioRequestDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setUsername(dto.getUsername());
        usuario.setPassword(dto.getPassword());
        usuario.setRol(dto.getRol());

        return usuarioService.crear(usuario);
    }
}