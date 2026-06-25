package com.example.backendastramaco.controller;

import com.example.backendastramaco.dto.PedidoRequestDTO;
import com.example.backendastramaco.dto.PedidoResponseDTO;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.enums.EstadoPedido;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "API para la gestión de pedidos de tarjetas")
public class PedidoController {

    private final PedidoService pedidoService;
    private final UsuarioRepository usuarioRepository;
    private final TransportistaRepository transportistaRepository;

    @PostMapping
    @Operation(summary = "Crear un nuevo pedido", description = "Registra un nuevo pedido de tarjetas en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pedido creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content)
    })
    public ResponseEntity<PedidoResponseDTO> crear(@Valid @RequestBody PedidoRequestDTO dto) {
        PedidoResponseDTO response = pedidoService.crearPedido(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar todos los pedidos", description = "Obtiene el listado completo de pedidos con paginación. Solo ADMIN puede acceder.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content)
    })
    public ResponseEntity<Page<PedidoResponseDTO>> listar(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long transportistaId) {
        Page<PedidoResponseDTO> pedidos = pedidoService.listar(pageable, estado, transportistaId);
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/todos")
    @Operation(summary = "Listar todos los pedidos incluyendo eliminados", description = "Obtiene todos los pedidos incluyendo eliminados lógicamente (solo ADMIN)")
    public ResponseEntity<Page<PedidoResponseDTO>> listarTodos(@PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<PedidoResponseDTO> pedidos = pedidoService.listarTodos(pageable);
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/eliminados")
    @Operation(summary = "Listar pedidos eliminados", description = "Obtiene solo los pedidos eliminados lógicamente (solo ADMIN)")
    public ResponseEntity<Page<PedidoResponseDTO>> listarEliminados(@PageableDefault(size = 10, sort = "deletedAt") Pageable pageable) {
        Page<PedidoResponseDTO> pedidos = pedidoService.listarEliminados(pageable);
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener pedido por ID", description = "Obtiene un pedido específico por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado", content = @Content)
    })
    public ResponseEntity<PedidoResponseDTO> obtenerPorId(@PathVariable Long id) {
        PedidoResponseDTO pedido = pedidoService.obtenerPorId(id);
        return ResponseEntity.ok(pedido);
    }

    @GetMapping("/me")
    @Operation(summary = "Listar mis pedidos", description = "Obtiene los pedidos del transportista autenticado con paginación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedidos obtenidos"),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content)
    })
    public ResponseEntity<Page<PedidoResponseDTO>> listarMisPedidos(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) String estado) {
        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Transportista transportista = transportistaRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Transportista no encontrado"));
        Page<PedidoResponseDTO> pedidos = pedidoService.listarPorTransportista(transportista.getId(), pageable, estado);
        return ResponseEntity.ok(pedidos);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar pedido", description = "Actualiza los datos de un pedido existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido actualizado"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    public ResponseEntity<PedidoResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PedidoRequestDTO dto) {
        PedidoResponseDTO response = pedidoService.actualizar(id, dto);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado del pedido", description = "Actualiza el estado de un pedido (EN_ENVIO, ENTREGADO, CANCELADO)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Estado inválido", content = @Content)
    })
    public ResponseEntity<PedidoResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestParam String estado) {
        PedidoResponseDTO response = pedidoService.cambiarEstado(id, estado);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar pedido (soft delete)", description = "Elimina lógicamente un pedido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Pedido eliminado lógicamente"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado", content = @Content)
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        pedidoService.eliminar(id, username);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restaurar")
    @Operation(summary = "Restaurar pedido", description = "Restaura un pedido eliminado lógicamente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Pedido restaurado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado", content = @Content)
    })
    public ResponseEntity<Void> restaurar(@PathVariable Long id) {
        pedidoService.restaurar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permanente")
    @Operation(summary = "Eliminar pedido permanentemente", description = "Elimina físicamente un pedido de la base de datos (solo ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Pedido eliminado permanentemente"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado", content = @Content)
    })
    public ResponseEntity<Void> eliminarPermanente(@PathVariable Long id) {
        pedidoService.eliminarPermanente(id);
        return ResponseEntity.noContent().build();
    }
}