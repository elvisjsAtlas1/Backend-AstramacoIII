package com.example.backendastramaco.controller;

import com.example.backendastramaco.model.DocumentoPersonal;
import com.example.backendastramaco.service.DocumentoPersonalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoPersonalController {

    private final DocumentoPersonalService service;

    @PostMapping("/{transportistaId}")
    public DocumentoPersonal guardar(
            @PathVariable Long transportistaId,
            @RequestBody DocumentoPersonal doc) {

        return service.guardar(transportistaId, doc);
    }

    @GetMapping("/transportista/{id}")
    public List<DocumentoPersonal> listar(@PathVariable Long id) {
        return service.listarPorTransportista(id);
    }
}