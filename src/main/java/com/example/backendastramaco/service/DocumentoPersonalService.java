package com.example.backendastramaco.service;

import com.example.backendastramaco.model.DocumentoPersonal;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.repository.DocumentoPersonalRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.service.audit.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentoPersonalService {

    private final DocumentoPersonalRepository repository;
    private final TransportistaRepository transportistaRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Transactional
    public DocumentoPersonal guardar(Long transportistaId, DocumentoPersonal doc) {

        Transportista t = transportistaRepository.findById(transportistaId)
                .orElseThrow(() -> new RuntimeException("Transportista no existe"));

        // validar duplicado
        boolean existe = repository.existsByTransportistaIdAndTipoDocumento(
                transportistaId, doc.getTipoDocumento());

        if (existe) {
            throw new IllegalArgumentException("Documento ya registrado");
        }

        validarDocumento(doc);

        doc.setTransportista(t);

        DocumentoPersonal saved = repository.save(doc);

        // Auditar creación
        auditService.auditDocumento(
                saved.getId(),
                transportistaId,
                "CREATE",
                null,
                saved,
                request
        );

        return saved;
    }

    public List<DocumentoPersonal> listarPorTransportista(Long id) {
        return repository.findByTransportistaId(id);
    }

    @Transactional
    public DocumentoPersonal actualizar(Long documentoId, DocumentoPersonal nuevosDatos) {
        DocumentoPersonal existente = repository.findById(documentoId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        // Guardar copia del estado anterior
        DocumentoPersonal oldCopy = copiarEntidad(existente);

        // Actualizar campos
        existente.setTipoDocumento(nuevosDatos.getTipoDocumento());
        existente.setValor(nuevosDatos.getValor());
        existente.setFechaEmision(nuevosDatos.getFechaEmision());
        existente.setFechaVencimiento(nuevosDatos.getFechaVencimiento());
        existente.setActivo(nuevosDatos.getActivo());

        validarDocumento(existente);

        DocumentoPersonal updated = repository.save(existente);

        // Auditar actualización
        auditService.auditDocumento(
                documentoId,
                existente.getTransportista().getId(),
                "UPDATE",
                oldCopy,
                updated,
                request
        );

        return updated;
    }

    @Transactional
    public void eliminar(Long documentoId, String username) {
        DocumentoPersonal existente = repository.findById(documentoId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        Long transportistaId = existente.getTransportista().getId();
        DocumentoPersonal oldCopy = copiarEntidad(existente);

        // Soft delete
        existente.setActivo(false);
        repository.save(existente);

        // Auditar eliminación
        auditService.auditDocumento(
                documentoId,
                transportistaId,
                "DELETE",
                oldCopy,
                null,
                request
        );
    }

    private void validarDocumento(DocumentoPersonal doc) {

        switch (doc.getTipoDocumento()) {

            case SOAT, REVISION_TECNICA:
                if (doc.getFechaVencimiento() == null) {
                    throw new IllegalArgumentException("Requiere fecha");
                }
                break;

            case LICENCIA, TARJETA_CIRCULACION:
                if (!doc.getValor().equalsIgnoreCase("SI") &&
                        !doc.getValor().equalsIgnoreCase("NO")) {
                    throw new IllegalArgumentException("Debe ser SI o NO");
                }
                break;

            default:
                break;
        }
    }

    private DocumentoPersonal copiarEntidad(DocumentoPersonal original) {
        DocumentoPersonal copia = new DocumentoPersonal();
        copia.setId(original.getId());
        copia.setTipoDocumento(original.getTipoDocumento());
        copia.setValor(original.getValor());
        copia.setFechaEmision(original.getFechaEmision());
        copia.setFechaVencimiento(original.getFechaVencimiento());
        copia.setActivo(original.getActivo());
        copia.setTransportista(original.getTransportista());
        return copia;
    }
}