package com.example.backendastramaco.controller;

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
    private Usuario usuario;

    @PostMapping
    public Usuario crear(@RequestBody Usuario usuario) {
        this.usuario = usuario;
        return usuarioService.crear(usuario);
    }
}