package com.example.backendastramaco.controller;

import com.example.backendastramaco.dto.PedidoRequestDTO;
import com.example.backendastramaco.dto.PedidoResponseDTO;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.service.PedidoService;
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
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "API para la gestión de pedidos de tarjetas")
public class PedidoController {

    private final PedidoService service;
    private final UsuarioRepository usuarioRepository;
    private final TransportistaRepository transportistaRepository;

    @PostMapping
    @Operation(summary = "Crear un nuevo pedido", description = "Registra un nuevo pedido de tarjetas en el sistema. Requiere transportistaId, tipoTarjeta y cantidad.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    public PedidoResponseDTO crear(@RequestBody PedidoRequestDTO dto) {
        return service.crearPedido(dto);
    }

    @GetMapping
    @Operation(summary = "Listar todos los pedidos", description = "Obtiene el listado completo de pedidos. Solo ADMIN puede acceder.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content)
    })
    public List<PedidoResponseDTO> listar() {
        return service.listar();
    }

    @GetMapping("/me")
    @Operation(summary = "Listar mis pedidos", description = "Obtiene los pedidos del transportista autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedidos obtenidos"),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content)
    })
    public List<PedidoResponseDTO> listarMisPedidos(Authentication authentication) {
        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Transportista transportista = transportistaRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Transportista no encontrado"));
        return service.listarPorTransportista(transportista.getId());
    }
}