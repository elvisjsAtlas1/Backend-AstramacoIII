package com.example.backendastramaco.controller.auditoria;

import com.example.backendastramaco.model.audit.AuditoriaUsuario;
import com.example.backendastramaco.repository.audit.AuditoriaUsuarioRepository;
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
@RequestMapping("/api/auditoria/usuarios")
@RequiredArgsConstructor
public class AuditoriaUsuarioController {
    private final AuditoriaUsuarioRepository repository;

    @GetMapping
    public ResponseEntity<Page<AuditoriaUsuario>> listar(@PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<Page<AuditoriaUsuario>> listarPorUsuario(
            @PathVariable Long usuarioId,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findByUsuarioIdOrderByFechaHoraDesc(usuarioId, pageable));
    }

    @GetMapping("/accion/{accion}")
    public ResponseEntity<Page<AuditoriaUsuario>> listarPorAccion(
            @PathVariable String accion,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findByAccion(accion, pageable));
    }

    @GetMapping("/fechas")
    public ResponseEntity<Page<AuditoriaUsuario>> listarPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(repository.findByFechaHoraBetween(inicio, fin, pageable));
    }

    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Long>> getResumenAuditorias() {
        Map<String, Long> resumen = new HashMap<>();
        resumen.put("CREATE", repository.countByAccion("CREATE"));
        resumen.put("UPDATE", repository.countByAccion("UPDATE"));
        resumen.put("DELETE", repository.countByAccion("DELETE"));
        resumen.put("RESTORE", repository.countByAccion("RESTORE"));
        resumen.put("DELETE_PERMANENT", repository.countByAccion("DELETE_PERMANENT"));
        return ResponseEntity.ok(resumen);
    }
}