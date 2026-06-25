package com.example.backendastramaco.controller;

import com.example.backendastramaco.dto.UsuarioRequestDTO;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.service.UsuarioService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "API para la gestión de usuarios del sistema")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @Operation(summary = "Crear usuario", description = "Registra un nuevo usuario con rol específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "Usuario ya existe", content = @Content)
    })
    public ResponseEntity<Usuario> crear(@Valid @RequestBody UsuarioRequestDTO dto) {
        Usuario usuario = usuarioService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Obtiene todos los usuarios activos con paginación y filtros")
    public ResponseEntity<Page<Usuario>> listar(
            @PageableDefault(size = 10, sort = "username") Pageable pageable,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo) {
        Page<Usuario> usuarios = usuarioService.listar(pageable, rol, activo);
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/todos")
    @Operation(summary = "Listar todos los usuarios", description = "Obtiene todos los usuarios incluyendo eliminados lógicamente")
    public ResponseEntity<Page<Usuario>> listarTodos(
            @PageableDefault(size = 10, sort = "username") Pageable pageable) {
        Page<Usuario> usuarios = usuarioService.listarTodos(pageable);
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/eliminados")
    @Operation(summary = "Listar usuarios eliminados", description = "Obtiene solo los usuarios eliminados lógicamente")
    public ResponseEntity<Page<Usuario>> listarEliminados(
            @PageableDefault(size = 10, sort = "deletedAt") Pageable pageable) {
        Page<Usuario> usuarios = usuarioService.listarEliminados(pageable);
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID", description = "Obtiene un usuario específico por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    })
    public ResponseEntity<Usuario> obtenerPorId(@PathVariable Long id) {
        Usuario usuario = usuarioService.obtenerPorId(id);
        return ResponseEntity.ok(usuario);
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Obtener usuario por username", description = "Obtiene un usuario específico por su username")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    })
    public ResponseEntity<Usuario> obtenerPorUsername(@PathVariable String username) {
        Usuario usuario = usuarioService.obtenerPorUsername(username);
        return ResponseEntity.ok(usuario);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario", description = "Actualiza los datos de un usuario existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario actualizado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    public ResponseEntity<Usuario> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequestDTO dto) {
        Usuario usuario = usuarioService.actualizar(id, dto);
        return ResponseEntity.ok(usuario);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado del usuario", description = "Activa o desactiva un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    })
    public ResponseEntity<Void> cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        usuarioService.cambiarEstado(id, activo);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario (soft delete)", description = "Elimina lógicamente un usuario por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuario eliminado lógicamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restaurar")
    @Operation(summary = "Restaurar usuario", description = "Restaura un usuario eliminado lógicamente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuario restaurado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    })
    public ResponseEntity<Void> restaurar(@PathVariable Long id) {
        usuarioService.restaurar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permanente")
    @Operation(summary = "Eliminar usuario permanentemente", description = "Elimina físicamente un usuario de la base de datos (solo para administradores)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuario eliminado permanentemente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    })
    public ResponseEntity<Void> eliminarPermanente(@PathVariable Long id) {
        usuarioService.eliminarPermanente(id);
        return ResponseEntity.noContent().build();
    }
}