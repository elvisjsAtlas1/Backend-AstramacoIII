package com.example.backendastramaco.controller;

import com.example.backendastramaco.dto.TransportistaRequestDTO;
import com.example.backendastramaco.model.DocumentoPersonal;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.enums.TipoTransporte;

import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.service.DocumentoPersonalService;
import com.example.backendastramaco.service.TransportistaService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transportistas")
@RequiredArgsConstructor
public class TransportistaController {

    private final TransportistaService service;
    private final DocumentoPersonalService documentoService;
    private final UsuarioRepository usuarioRepository;
    private final TransportistaRepository transportistaRepository;

    @PostMapping
    public Transportista crear(@RequestBody TransportistaRequestDTO dto) {
        return service.crear(dto);
    }

    @GetMapping
    public List<Transportista> listar() {
        return service.listar();
    }

    @GetMapping("/tipo/{tipo}")
    public List<Transportista> listarPorTipo(@PathVariable TipoTransporte tipo) {
        return service.listarPorTipo(tipo);
    }

    @GetMapping("/{id}/documentos")
    public List<DocumentoPersonal> documentos(@PathVariable Long id) {
        return documentoService.listarPorTransportista(id);
    }

    @GetMapping("/me")
    public Transportista obtenerMiPerfil(Authentication authentication) {

        if (authentication == null) {
            throw new IllegalArgumentException("No autenticado");
        }

        String username = authentication.getName();

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return transportistaRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Transportista no encontrado"));
    }

}