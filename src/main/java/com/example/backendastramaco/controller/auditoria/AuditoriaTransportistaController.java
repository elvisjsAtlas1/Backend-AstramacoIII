package com.example.backendastramaco.controller.auditoria;

import com.example.backendastramaco.model.audit.AuditoriaTransportista;
import com.example.backendastramaco.repository.audit.AuditoriaTransportistaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria/transportistas")
@RequiredArgsConstructor
public class AuditoriaTransportistaController {
    private final AuditoriaTransportistaRepository repository;

    @GetMapping
    public List<AuditoriaTransportista> listar() {
        return repository.findAllByOrderByFechaHoraDesc();
    }

    @GetMapping("/transportista/{transportistaId}")
    public List<AuditoriaTransportista> listarPorTransportista(@PathVariable Long transportistaId) {
        return repository.findByTransportistaIdOrderByFechaHoraDesc(transportistaId);
    }
}