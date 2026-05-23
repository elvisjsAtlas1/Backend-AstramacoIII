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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
@Tag(name = "Documentos", description = "API para la gestión de documentos personales de transportistas")
public class DocumentoPersonalController {

    private final DocumentoPersonalService service;

    @PostMapping("/{transportistaId}")
    @Operation(summary = "Guardar documento", description = "Registra un nuevo documento personal para un transportista específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documento guardado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content)
    })
    public DocumentoPersonal guardar(
            @Parameter(description = "ID del transportista", example = "1", required = true)
            @PathVariable Long transportistaId,
            @Parameter(description = "Datos del documento", required = true)
            @RequestBody DocumentoPersonalRequestDTO dto) {

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(dto.getTipoDocumento());
        doc.setValor(dto.getValor());
        doc.setFechaVencimiento(dto.getFechaVencimiento());

        return service.guardar(transportistaId, doc);
    }

    @GetMapping("/transportista/{id}")
    @Operation(summary = "Listar documentos", description = "Obtiene todos los documentos de un transportista por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documentos obtenidos exitosamente"),
            @ApiResponse(responseCode = "404", description = "Transportista no encontrado", content = @Content)
    })
    public List<DocumentoPersonal> listar(
            @Parameter(description = "ID del transportista", example = "1", required = true)
            @PathVariable Long id) {
        return service.listarPorTransportista(id);
    }
}