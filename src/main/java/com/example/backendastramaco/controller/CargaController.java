package com.example.backendastramaco.controller;

import com.example.backendastramaco.dto.AumentarCargaRequestDTO;
import com.example.backendastramaco.dto.CargaRequestDTO;
import com.example.backendastramaco.dto.CargaResponseDTO;
import com.example.backendastramaco.service.CargaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cargas")
@RequiredArgsConstructor
public class CargaController {

    private final CargaService service;

    @PutMapping("/{transportistaId}")
    public CargaResponseDTO subirCargaActual(
            @PathVariable Long transportistaId,
            @Valid @RequestBody CargaRequestDTO request) {
        return service.subirCargaActual(transportistaId, request);
    }

    @PostMapping("/{transportistaId}/aumentar")
    public CargaResponseDTO aumentarCargaActual(
            @PathVariable Long transportistaId,
            @Valid @RequestBody AumentarCargaRequestDTO request) {
        return service.aumentarCargaActual(transportistaId, request);
    }

    @GetMapping("/{transportistaId}")
    public CargaResponseDTO obtenerCarga(@PathVariable Long transportistaId) {
        return service.obtenerCarga(transportistaId);
    }

    @GetMapping
    public List<CargaResponseDTO> listarTodas() {
        return service.listarTodas();
    }
}