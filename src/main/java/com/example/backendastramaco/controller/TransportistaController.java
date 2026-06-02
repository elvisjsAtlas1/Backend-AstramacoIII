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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transportistas")
@RequiredArgsConstructor
@Tag(name = "Transportistas", description = "API para la gestión de transportistas")
public class TransportistaController {

    private final TransportistaService service;
    private final DocumentoPersonalService documentoService;
    private final UsuarioRepository usuarioRepository;
    private final TransportistaRepository transportistaRepository;

    @PostMapping
    @Operation(summary = "Registrar transportista", description = "Crea un nuevo transportista asociado a un usuario existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transportista registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    public Transportista crear(@RequestBody TransportistaRequestDTO dto) {
        return service.crear(dto);
    }

    @GetMapping
    @Operation(summary = "Listar transportistas", description = "Obtiene todos los transportistas (solo ADMIN)")
    public List<Transportista> listar() {
        return service.listar();
    }

    @GetMapping("/tipo/{tipo}")
    @Operation(summary = "Buscar por tipo", description = "Filtra transportistas por tipo de transporte")
    public List<Transportista> listarPorTipo(@PathVariable TipoTransporte tipo) {
        return service.listarPorTipo(tipo);
    }

    @GetMapping("/{id}/documentos")
    @Operation(summary = "Ver documentos", description = "Obtiene documentos de un transportista")
    public List<DocumentoPersonal> documentos(@PathVariable Long id) {
        return documentoService.listarPorTransportista(id);
    }

    @GetMapping("/me")
    @Operation(summary = "Mi perfil", description = "Obtiene el perfil del transportista autenticado")
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

    // Agregar a TransportistaController.java

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar transportista", description = "Actualiza los datos de un transportista existente")
    public Transportista actualizar(@PathVariable Long id, @RequestBody TransportistaRequestDTO dto) {
        return service.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar transportista", description = "Elimina lógicamente un transportista")
    public void eliminar(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        service.eliminar(id, username);
    }
}