package com.example.backendastramaco.controller;

import com.example.backendastramaco.dto.AumentarCargaRequestDTO;
import com.example.backendastramaco.dto.CargaRequestDTO;
import com.example.backendastramaco.dto.CargaResponseDTO;
import com.example.backendastramaco.service.CargaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/cargas")
@RequiredArgsConstructor
@Tag(name = "Cargas", description = "API para la gestión de cargas de tarjetas asignadas a transportistas")
public class CargaController {

    private final CargaService cargaService;

    @PostMapping("/{transportistaId}")
    @Operation(summary = "Crear carga", description = "Crea una nueva carga para un transportista")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Carga creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Carga ya existe", content = @Content)
    })
    public ResponseEntity<CargaResponseDTO> crearCarga(
            @Parameter(description = "ID del transportista", example = "1", required = true)
            @PathVariable Long transportistaId,
            @Valid @RequestBody CargaRequestDTO request) {
        CargaResponseDTO response = cargaService.crearCarga(transportistaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{transportistaId}")
    @Operation(summary = "Actualizar carga", description = "Actualiza o asigna una nueva carga a un transportista")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carga actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content)
    })
    public ResponseEntity<CargaResponseDTO> actualizarCarga(
            @Parameter(description = "ID del transportista", example = "1", required = true)
            @PathVariable Long transportistaId,
            @Valid @RequestBody CargaRequestDTO request) {
        CargaResponseDTO response = cargaService.actualizarCarga(transportistaId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{transportistaId}/aumentar")
    @Operation(summary = "Aumentar carga", description = "Incrementa la cantidad disponible de una carga existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carga aumentada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Cantidad inválida o sin inventario", content = @Content),
            @ApiResponse(responseCode = "404", description = "Carga o transportista no encontrado", content = @Content)
    })
    public ResponseEntity<CargaResponseDTO> aumentarCarga(
            @Parameter(description = "ID del transportista", example = "1", required = true)
            @PathVariable Long transportistaId,
            @Valid @RequestBody AumentarCargaRequestDTO request) {
        CargaResponseDTO response = cargaService.aumentarCarga(transportistaId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transportistaId}")
    @Operation(summary = "Obtener carga", description = "Obtiene la carga actual de un transportista específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carga obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Carga o transportista no encontrado", content = @Content)
    })
    public ResponseEntity<CargaResponseDTO> obtenerCarga(
            @Parameter(description = "ID del transportista", example = "1", required = true)
            @PathVariable Long transportistaId) {
        CargaResponseDTO response = cargaService.obtenerCarga(transportistaId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/detalle")
    @Operation(summary = "Obtener carga por ID", description = "Obtiene una carga específica por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carga encontrada"),
            @ApiResponse(responseCode = "404", description = "Carga no encontrada", content = @Content)
    })
    public ResponseEntity<CargaResponseDTO> obtenerPorId(@PathVariable Long id) {
        CargaResponseDTO response = cargaService.obtenerPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Listar todas las cargas", description = "Obtiene el listado completo de todas las cargas con paginación (solo ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de cargas obtenida"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content)
    })
    public ResponseEntity<Page<CargaResponseDTO>> listarTodas(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) String tipoMaterial) {
        Page<CargaResponseDTO> cargas = cargaService.listarTodas(pageable, tipoMaterial);
        return ResponseEntity.ok(cargas);
    }

    @GetMapping("/transportista/{transportistaId}/historial")
    @Operation(summary = "Historial de cargas", description = "Obtiene el historial de cargas de un transportista (solo ADMIN)")
    public ResponseEntity<Page<CargaResponseDTO>> listarHistorialPorTransportista(
            @PathVariable Long transportistaId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<CargaResponseDTO> cargas = cargaService.listarHistorialPorTransportista(transportistaId, pageable);
        return ResponseEntity.ok(cargas);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar carga (soft delete)", description = "Elimina lógicamente una carga")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Carga eliminada lógicamente"),
            @ApiResponse(responseCode = "404", description = "Carga no encontrada", content = @Content)
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        cargaService.eliminar(id, username);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restaurar")
    @Operation(summary = "Restaurar carga", description = "Restaura una carga eliminada lógicamente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Carga restaurada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Carga no encontrada", content = @Content)
    })
    public ResponseEntity<Void> restaurar(@PathVariable Long id) {
        cargaService.restaurar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permanente")
    @Operation(summary = "Eliminar carga permanentemente", description = "Elimina físicamente una carga de la base de datos (solo ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Carga eliminada permanentemente"),
            @ApiResponse(responseCode = "404", description = "Carga no encontrada", content = @Content)
    })
    public ResponseEntity<Void> eliminarPermanente(@PathVariable Long id) {
        cargaService.eliminarPermanente(id);
        return ResponseEntity.noContent().build();
    }
}