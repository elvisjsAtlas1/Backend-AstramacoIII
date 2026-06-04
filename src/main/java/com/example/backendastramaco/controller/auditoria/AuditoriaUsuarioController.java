package com.example.backendastramaco.controller.auditoria;

import com.example.backendastramaco.model.audit.AuditoriaUsuario;
import com.example.backendastramaco.repository.audit.AuditoriaUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria/usuarios")
@RequiredArgsConstructor
public class AuditoriaUsuarioController {
    private final AuditoriaUsuarioRepository repository;

    @GetMapping
    public List<AuditoriaUsuario> listar() {
        return repository.findAllByOrderByFechaHoraDesc();
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<AuditoriaUsuario> listarPorUsuario(@PathVariable Long usuarioId) {
        return repository.findByUsuarioIdOrderByFechaHoraDesc(usuarioId);
    }
}