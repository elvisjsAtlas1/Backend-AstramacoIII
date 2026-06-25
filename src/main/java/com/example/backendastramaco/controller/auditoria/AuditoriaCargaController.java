package com.example.backendastramaco.controller.auditoria;

import com.example.backendastramaco.model.audit.AuditoriaCarga;
import com.example.backendastramaco.repository.audit.AuditoriaCargaRepository;
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
@RequestMapping("/api/auditoria/cargas")
@RequiredArgsConstructor
@Tag(name = "Auditoría Cargas", description = "API para consultar la auditoría de cargas")
public class AuditoriaCargaController {

    private final AuditoriaCargaRepository repository;

    @GetMapping
    @Operation(summary = "Listar todas las auditorías de cargas")
    public ResponseEntity<Page<AuditoriaCarga>> listar(
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/carga/{cargaId}")
    @Operation(summary = "Auditorías por carga")
    public ResponseEntity<Page<AuditoriaCarga>> listarPorCarga(
            @PathVariable Long cargaId,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findByCargaIdOrderByFechaHoraDesc(cargaId, pageable));
    }

    @GetMapping("/transportista/{transportistaId}")
    @Operation(summary = "Auditorías por transportista")
    public ResponseEntity<Page<AuditoriaCarga>> listarPorTransportista(
            @PathVariable Long transportistaId,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findByTransportistaIdOrderByFechaHoraDesc(transportistaId, pageable));
    }

    @GetMapping("/accion/{accion}")
    @Operation(summary = "Auditorías por acción")
    public ResponseEntity<Page<AuditoriaCarga>> listarPorAccion(
            @PathVariable String accion,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findByAccion(accion, pageable));
    }

    @GetMapping("/fechas")
    @Operation(summary = "Auditorías por rango de fechas")
    public ResponseEntity<Page<AuditoriaCarga>> listarPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findByFechaHoraBetween(inicio, fin, pageable));
    }

    @GetMapping("/resumen")
    @Operation(summary = "Resumen de auditorías de cargas")
    public ResponseEntity<Map<String, Long>> getResumenAuditorias() {
        Map<String, Long> resumen = new HashMap<>();
        resumen.put("CREATE", repository.countByAccion("CREATE"));
        resumen.put("UPDATE", repository.countByAccion("UPDATE"));
        resumen.put("UPDATE_AUMENTO", repository.countByAccion("UPDATE_AUMENTO"));
        resumen.put("DELETE", repository.countByAccion("DELETE"));
        resumen.put("RESTORE", repository.countByAccion("RESTORE"));
        resumen.put("DELETE_PERMANENT", repository.countByAccion("DELETE_PERMANENT"));
        return ResponseEntity.ok(resumen);
    }
}