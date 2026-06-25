package com.example.backendastramaco.controller;

import com.example.backendastramaco.dto.DocumentoPersonalRequestDTO;
import com.example.backendastramaco.model.DocumentoPersonal;
import com.example.backendastramaco.service.DocumentoPersonalService;
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

import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
@Tag(name = "Documentos", description = "API para la gestión de documentos personales de transportistas")
public class DocumentoPersonalController {

    private final DocumentoPersonalService documentoService;

    @PostMapping("/{transportistaId}")
    @Operation(summary = "Guardar documento", description = "Registra un nuevo documento personal para un transportista específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Documento guardado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Documento ya existe", content = @Content)
    })
    public ResponseEntity<DocumentoPersonal> guardar(
            @Parameter(description = "ID del transportista", example = "1", required = true)
            @PathVariable Long transportistaId,
            @Parameter(description = "Datos del documento", required = true)
            @Valid @RequestBody DocumentoPersonalRequestDTO dto) {

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(dto.getTipoDocumento());
        doc.setValor(dto.getValor());
        doc.setFechaEmision(dto.getFechaEmision());
        doc.setFechaVencimiento(dto.getFechaVencimiento());

        DocumentoPersonal saved = documentoService.guardar(transportistaId, doc);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener documento por ID", description = "Obtiene un documento específico por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documento encontrado"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado", content = @Content)
    })
    public ResponseEntity<DocumentoPersonal> obtenerPorId(@PathVariable Long id) {
        DocumentoPersonal documento = documentoService.obtenerPorId(id);
        return ResponseEntity.ok(documento);
    }

    @GetMapping("/transportista/{id}")
    @Operation(summary = "Listar documentos", description = "Obtiene todos los documentos de un transportista por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documentos obtenidos exitosamente"),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content)
    })
    public ResponseEntity<List<DocumentoPersonal>> listar(
            @Parameter(description = "ID del transportista", example = "1", required = true)
            @PathVariable Long id) {
        List<DocumentoPersonal> documentos = documentoService.listarPorTransportista(id);
        return ResponseEntity.ok(documentos);
    }

    @GetMapping("/transportista/{id}/paginado")
    @Operation(summary = "Listar documentos con paginación", description = "Obtiene todos los documentos de un transportista con paginación")
    public ResponseEntity<Page<DocumentoPersonal>> listarPaginado(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<DocumentoPersonal> documentos = documentoService.listarPorTransportistaPaginado(id, pageable);
        return ResponseEntity.ok(documentos);
    }

    @GetMapping("/transportista/{id}/activos")
    @Operation(summary = "Listar documentos activos", description = "Obtiene solo los documentos activos de un transportista")
    public ResponseEntity<List<DocumentoPersonal>> listarActivos(@PathVariable Long id) {
        List<DocumentoPersonal> documentos = documentoService.listarActivosPorTransportista(id);
        return ResponseEntity.ok(documentos);
    }

    @GetMapping("/me")
    @Operation(summary = "Mis documentos", description = "Obtiene los documentos del transportista autenticado")
    public ResponseEntity<List<DocumentoPersonal>> misDocumentos(Authentication authentication) {
        List<DocumentoPersonal> documentos = documentoService.listarMisDocumentos(authentication);
        return ResponseEntity.ok(documentos);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar documento", description = "Actualiza los datos de un documento existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documento actualizado"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    public ResponseEntity<DocumentoPersonal> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DocumentoPersonalRequestDTO dto) {

        DocumentoPersonal nuevosDatos = new DocumentoPersonal();
        nuevosDatos.setTipoDocumento(dto.getTipoDocumento());
        nuevosDatos.setValor(dto.getValor());
        nuevosDatos.setFechaEmision(dto.getFechaEmision());
        nuevosDatos.setFechaVencimiento(dto.getFechaVencimiento());

        DocumentoPersonal updated = documentoService.actualizar(id, nuevosDatos);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado del documento", description = "Activa o desactiva un documento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado", content = @Content)
    })
    public ResponseEntity<Void> cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        documentoService.cambiarEstado(id, activo);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar documento (soft delete)", description = "Elimina lógicamente un documento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Documento eliminado lógicamente"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado", content = @Content)
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        documentoService.eliminar(id, username);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restaurar")
    @Operation(summary = "Restaurar documento", description = "Restaura un documento eliminado lógicamente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Documento restaurado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado", content = @Content)
    })
    public ResponseEntity<Void> restaurar(@PathVariable Long id) {
        documentoService.restaurar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permanente")
    @Operation(summary = "Eliminar documento permanentemente", description = "Elimina físicamente un documento de la base de datos (solo ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Documento eliminado permanentemente"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado", content = @Content)
    })
    public ResponseEntity<Void> eliminarPermanente(@PathVariable Long id) {
        documentoService.eliminarPermanente(id);
        return ResponseEntity.noContent().build();
    }
}