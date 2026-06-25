package com.example.backendastramaco.controller.auditoria;

import com.example.backendastramaco.model.audit.AuditoriaDocumento;
import com.example.backendastramaco.repository.audit.AuditoriaDocumentoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auditoria/documentos")
@RequiredArgsConstructor
@Tag(name = "Auditoría Documentos", description = "API para consultar la auditoría de documentos")
public class AuditoriaDocumentoController {

    private final AuditoriaDocumentoRepository repository;

    @GetMapping
    @Operation(summary = "Listar todas las auditorías de documentos")
    public ResponseEntity<Page<AuditoriaDocumento>> listar(
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/documento/{documentoId}")
    @Operation(summary = "Auditorías por documento")
    public ResponseEntity<Page<AuditoriaDocumento>> listarPorDocumento(
            @PathVariable Long documentoId,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findByDocumentoIdOrderByFechaHoraDesc(documentoId, pageable));
    }

    @GetMapping("/transportista/{transportistaId}")
    @Operation(summary = "Auditorías por transportista")
    public ResponseEntity<Page<AuditoriaDocumento>> listarPorTransportista(
            @PathVariable Long transportistaId,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findByTransportistaIdOrderByFechaHoraDesc(transportistaId, pageable));
    }

    @GetMapping("/accion/{accion}")
    @Operation(summary = "Auditorías por acción")
    public ResponseEntity<Page<AuditoriaDocumento>> listarPorAccion(
            @PathVariable String accion,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findByAccion(accion, pageable));
    }

    @GetMapping("/fechas")
    @Operation(summary = "Auditorías por rango de fechas")
    public ResponseEntity<Page<AuditoriaDocumento>> listarPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findByFechaHoraBetween(inicio, fin, pageable));
    }

    @GetMapping("/resumen")
    @Operation(summary = "Resumen de auditorías de documentos")
    public ResponseEntity<Map<String, Long>> getResumenAuditorias() {
        Map<String, Long> resumen = new HashMap<>();
        resumen.put("CREATE", repository.countByAccion("CREATE"));
        resumen.put("UPDATE", repository.countByAccion("UPDATE"));
        resumen.put("DELETE", repository.countByAccion("DELETE"));
        resumen.put("RESTORE", repository.countByAccion("RESTORE"));
        resumen.put("DELETE_PERMANENT", repository.countByAccion("DELETE_PERMANENT"));
        resumen.put("UPDATE_ESTADO", repository.countByAccion("UPDATE_ESTADO"));
        return ResponseEntity.ok(resumen);
    }
}