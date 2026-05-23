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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cargas")
@RequiredArgsConstructor
@Tag(name = "Cargas", description = "API para la gestión de cargas de tarjetas asignadas a transportistas")
public class CargaController {

    private final CargaService service;

    @PutMapping("/{transportistaId}")
    @Operation(summary = "Actualizar carga", description = "Actualiza o asigna una nueva carga a un transportista")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carga actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content)
    })
    public CargaResponseDTO subirCargaActual(
            @Parameter(description = "ID del transportista", example = "1", required = true)
            @PathVariable Long transportistaId,
            @Valid @RequestBody CargaRequestDTO request) {
        return service.subirCargaActual(transportistaId, request);
    }

    @PostMapping("/{transportistaId}/aumentar")
    @Operation(summary = "Aumentar carga", description = "Incrementa la cantidad disponible de una carga existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carga aumentada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Cantidad inválida o sin inventario", content = @Content),
            @ApiResponse(responseCode = "404", description = "Carga o transportista no encontrado", content = @Content)
    })
    public CargaResponseDTO aumentarCargaActual(
            @Parameter(description = "ID del transportista", example = "1", required = true)
            @PathVariable Long transportistaId,
            @Valid @RequestBody AumentarCargaRequestDTO request) {
        return service.aumentarCargaActual(transportistaId, request);
    }

    @GetMapping("/{transportistaId}")
    @Operation(summary = "Obtener carga", description = "Obtiene la carga actual de un transportista específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carga obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Carga o transportista no encontrado", content = @Content)
    })
    public CargaResponseDTO obtenerCarga(
            @Parameter(description = "ID del transportista", example = "1", required = true)
            @PathVariable Long transportistaId) {
        return service.obtenerCarga(transportistaId);
    }

    @GetMapping
    @Operation(summary = "Listar todas las cargas", description = "Obtiene el listado completo de todas las cargas (solo ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de cargas obtenida"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content)
    })
    public List<CargaResponseDTO> listarTodas() {
        return service.listarTodas();
    }
}