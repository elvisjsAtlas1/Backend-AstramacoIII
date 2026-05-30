package com.example.backendastramaco.service;

import com.example.backendastramaco.model.DocumentoPersonal;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.repository.DocumentoPersonalRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.service.audit.AuditService;
import jakarta.persistence.EntityNotFoundException;
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
    private final HttpServletRequest request; // SonarQube permite si el proxy de Spring está activo, pero vigila su uso

    @Transactional
    public DocumentoPersonal guardar(Long transportistaId, DocumentoPersonal doc) {
        Transportista t = transportistaRepository.findById(transportistaId)
                .orElseThrow(() -> new EntityNotFoundException("Transportista con ID " + transportistaId + " no existe"));

        boolean existe = repository.existsByTransportistaIdAndTipoDocumento(transportistaId, doc.getTipoDocumento());
        if (existe) {
            throw new IllegalArgumentException("El documento tipo " + doc.getTipoDocumento() + " ya está registrado");
        }

        validarDocumento(doc);
        doc.setTransportista(t);

        DocumentoPersonal saved = repository.save(doc);

        auditService.auditDocumento(saved.getId(), transportistaId, "CREATE", null, saved, request);
        return saved;
    }

    @Transactional(readOnly = true) // SonarQube: Optimiza el rendimiento de la DB para lecturas
    public List<DocumentoPersonal> listarPorTransportista(Long id) {
        return repository.findByTransportistaId(id);
    }

    @Transactional
    public DocumentoPersonal actualizar(Long documentoId, DocumentoPersonal nuevosDatos) {
        DocumentoPersonal existente = repository.findById(documentoId)
                .orElseThrow(() -> new EntityNotFoundException("Documento con ID " + documentoId + " no encontrado"));

        DocumentoPersonal oldCopy = copiarEntidad(existente);

        existente.setTipoDocumento(nuevosDatos.getTipoDocumento());
        existente.setValor(nuevosDatos.getValor());

        // 🔥 Aseguramos la mutabilidad de ambas fechas desde los nuevos datos
        existente.setFechaEmision(nuevosDatos.getFechaEmision());
        existente.setFechaVencimiento(nuevosDatos.getFechaVencimiento());
        existente.setActivo(nuevosDatos.getActivo());

        validarDocumento(existente);
        DocumentoPersonal updated = repository.save(existente);

        auditService.auditDocumento(documentoId, existente.getTransportista().getId(), "UPDATE", oldCopy, updated, request);
        return updated;
    }

    @Transactional
    public void eliminar(Long documentoId, String username) {
        DocumentoPersonal existente = repository.findById(documentoId)
                .orElseThrow(() -> new EntityNotFoundException("Documento con ID " + documentoId + " no encontrado"));

        Long transportistaId = existente.getTransportista().getId();
        DocumentoPersonal oldCopy = copiarEntidad(existente);

        existente.setActivo(false);
        repository.save(existente);

        auditService.auditDocumento(documentoId, transportistaId, "DELETE", oldCopy, null, request);
    }

    private void validarDocumento(DocumentoPersonal doc) {
        if (doc.getTipoDocumento() == null) {
            throw new IllegalArgumentException("El tipo de documento no puede ser nulo");
        }

        switch (doc.getTipoDocumento()) {
            case SOAT, REVISION_TECNICA:
                // 🔥 CORRECCIÓN: Validamos ambas fechas obligatoriamente para cumplir las reglas
                if (doc.getFechaEmision() == null || doc.getFechaVencimiento() == null) {
                    throw new IllegalArgumentException("SOAT y REVISIÓN TÉCNICA requieren obligatoriamente fecha de emisión y vencimiento");
                }
                break;

            case LICENCIA, TARJETA_CIRCULACION:
                if (doc.getValor() == null ||
                        (!doc.getValor().equalsIgnoreCase("SI") && !doc.getValor().equalsIgnoreCase("NO"))) {
                    throw new IllegalArgumentException("El valor para LICENCIA o TARJETA DE CIRCULACIÓN debe ser 'SI' o 'NO'");
                }
                break;

            default:
                break;
        }
    }

    private DocumentoPersonal copiarEntidad(DocumentoPersonal original) {
        DocumentoPersonal copia = new DocumentoPersonal();
        copia.setId(original.getId()); // Funciona sin errores porque usa el setter estándar de la herencia
        copia.setTipoDocumento(original.getTipoDocumento());
        copia.setValor(original.getValor());
        copia.setFechaEmision(original.getFechaEmision());
        copia.setFechaVencimiento(original.getFechaVencimiento());
        copia.setActivo(original.getActivo());
        copia.setTransportista(original.getTransportista());
        return copia;
    }
}