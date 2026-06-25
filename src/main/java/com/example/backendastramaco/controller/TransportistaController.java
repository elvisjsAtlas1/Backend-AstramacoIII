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
@RequestMapping("/api/transportistas")
@RequiredArgsConstructor
@Tag(name = "Transportistas", description = "API para la gestión de transportistas")
public class TransportistaController {

    private final TransportistaService transportistaService;
    private final DocumentoPersonalService documentoService;
    private final UsuarioRepository usuarioRepository;
    private final TransportistaRepository transportistaRepository;

    @PostMapping
    @Operation(summary = "Registrar transportista", description = "Crea un nuevo transportista asociado a un usuario existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transportista creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "DNI o placa ya registrados", content = @Content)
    })
    public ResponseEntity<Transportista> crear(@Valid @RequestBody TransportistaRequestDTO dto) {
        Transportista transportista = transportistaService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(transportista);
    }

    @GetMapping
    @Operation(summary = "Listar transportistas", description = "Obtiene todos los transportistas con paginación y filtros")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de transportistas obtenida exitosamente")
    })
    public ResponseEntity<Page<Transportista>> listar(
            @PageableDefault(size = 10, sort = "nombre") Pageable pageable,
            @RequestParam(required = false) String tipoTransporte,
            @RequestParam(required = false) String estado) {
        Page<Transportista> transportistas = transportistaService.listar(pageable, tipoTransporte, estado);
        return ResponseEntity.ok(transportistas);
    }

    @GetMapping("/todos")
    @Operation(summary = "Listar todos los transportistas", description = "Obtiene todos los transportistas incluyendo eliminados lógicamente (solo ADMIN)")
    public ResponseEntity<Page<Transportista>> listarTodos(@PageableDefault(size = 10, sort = "nombre") Pageable pageable) {
        Page<Transportista> transportistas = transportistaService.listarTodos(pageable);
        return ResponseEntity.ok(transportistas);
    }

    @GetMapping("/eliminados")
    @Operation(summary = "Listar transportistas eliminados", description = "Obtiene solo los transportistas eliminados lógicamente (solo ADMIN)")
    public ResponseEntity<Page<Transportista>> listarEliminados(@PageableDefault(size = 10, sort = "deletedAt") Pageable pageable) {
        Page<Transportista> transportistas = transportistaService.listarEliminados(pageable);
        return ResponseEntity.ok(transportistas);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener transportista por ID", description = "Obtiene un transportista específico por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transportista encontrado"),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content)
    })
    public ResponseEntity<Transportista> obtenerPorId(@PathVariable Long id) {
        Transportista transportista = transportistaService.obtenerPorId(id);
        return ResponseEntity.ok(transportista);
    }

    @GetMapping("/dni/{dni}")
    @Operation(summary = "Obtener transportista por DNI", description = "Obtiene un transportista específico por su DNI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transportista encontrado"),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content)
    })
    public ResponseEntity<Transportista> obtenerPorDni(@PathVariable String dni) {
        Transportista transportista = transportistaService.obtenerPorDni(dni);
        return ResponseEntity.ok(transportista);
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Obtener transportista por usuario", description = "Obtiene el transportista asociado a un usuario")
    public ResponseEntity<Transportista> obtenerPorUsuario(@PathVariable Long usuarioId) {
        Transportista transportista = transportistaService.obtenerPorUsuarioId(usuarioId);
        return ResponseEntity.ok(transportista);
    }

    @GetMapping("/tipo/{tipo}")
    @Operation(summary = "Buscar por tipo", description = "Filtra transportistas por tipo de transporte")
    public ResponseEntity<List<Transportista>> listarPorTipo(@PathVariable TipoTransporte tipo) {
        List<Transportista> transportistas = transportistaService.listarPorTipo(tipo);
        return ResponseEntity.ok(transportistas);
    }

    @GetMapping("/{id}/documentos")
    @Operation(summary = "Ver documentos", description = "Obtiene documentos de un transportista")
    public ResponseEntity<List<DocumentoPersonal>> documentos(@PathVariable Long id) {
        List<DocumentoPersonal> documentos = documentoService.listarPorTransportista(id);
        return ResponseEntity.ok(documentos);
    }

    @GetMapping("/me")
    @Operation(summary = "Mi perfil", description = "Obtiene el perfil del transportista autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content)
    })
    public ResponseEntity<Transportista> obtenerMiPerfil(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Transportista transportista = transportistaRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Transportista no encontrado"));
        return ResponseEntity.ok(transportista);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar transportista", description = "Actualiza los datos de un transportista existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transportista actualizado"),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "DNI o placa ya registrados", content = @Content)
    })
    public ResponseEntity<Transportista> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody TransportistaRequestDTO dto) {
        Transportista transportista = transportistaService.actualizar(id, dto);
        return ResponseEntity.ok(transportista);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado del transportista", description = "Activa o desactiva un transportista")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content)
    })
    public ResponseEntity<Void> cambiarEstado(
            @PathVariable Long id,
            @RequestParam String estado) {
        transportistaService.cambiarEstado(id, estado);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar transportista (soft delete)", description = "Elimina lógicamente un transportista")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Transportista eliminado lógicamente"),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content)
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        transportistaService.eliminar(id, username);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restaurar")
    @Operation(summary = "Restaurar transportista", description = "Restaura un transportista eliminado lógicamente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Transportista restaurado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content)
    })
    public ResponseEntity<Void> restaurar(@PathVariable Long id) {
        transportistaService.restaurar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permanente")
    @Operation(summary = "Eliminar transportista permanentemente", description = "Elimina físicamente un transportista de la base de datos (solo ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Transportista eliminado permanentemente"),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content)
    })
    public ResponseEntity<Void> eliminarPermanente(@PathVariable Long id) {
        transportistaService.eliminarPermanente(id);
        return ResponseEntity.noContent().build();
    }
}