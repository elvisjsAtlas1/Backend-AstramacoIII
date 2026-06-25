package com.example.backendastramaco.controller.auditoria;

import com.example.backendastramaco.model.audit.AuditoriaPedido;
import com.example.backendastramaco.repository.audit.AuditoriaPedidoRepository;
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
@RequestMapping("/api/auditoria/pedidos")
@RequiredArgsConstructor
@Tag(name = "Auditoría Pedidos", description = "API para consultar la auditoría de pedidos")
public class AuditoriaPedidoController {

    private final AuditoriaPedidoRepository repository;

    @GetMapping
    @Operation(summary = "Listar todas las auditorías de pedidos")
    public ResponseEntity<Page<AuditoriaPedido>> listar(
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/pedido/{pedidoId}")
    @Operation(summary = "Auditorías por pedido")
    public ResponseEntity<Page<AuditoriaPedido>> listarPorPedido(
            @PathVariable Long pedidoId,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findByPedidoIdOrderByFechaHoraDesc(pedidoId, pageable));
    }

    @GetMapping("/accion/{accion}")
    @Operation(summary = "Auditorías por acción")
    public ResponseEntity<Page<AuditoriaPedido>> listarPorAccion(
            @PathVariable String accion,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findByAccion(accion, pageable));
    }

    @GetMapping("/fechas")
    @Operation(summary = "Auditorías por rango de fechas")
    public ResponseEntity<Page<AuditoriaPedido>> listarPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findByFechaHoraBetween(inicio, fin, pageable));
    }

    @GetMapping("/resumen")
    @Operation(summary = "Resumen de auditorías de pedidos")
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