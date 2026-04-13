package com.example.backendastramaco.service;

import com.example.backendastramaco.model.DocumentoPersonal;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.repository.DocumentoPersonalRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentoPersonalService {

    private final DocumentoPersonalRepository repository;
    private final TransportistaRepository transportistaRepository;

    public DocumentoPersonal guardar(Long transportistaId, DocumentoPersonal doc) {

        Transportista t = transportistaRepository.findById(transportistaId)
                .orElseThrow(() -> new RuntimeException("Transportista no existe"));

        // 🔥 validar duplicado
        boolean existe = repository.existsByTransportistaIdAndTipoDocumento(
                transportistaId, doc.getTipoDocumento());

        if (existe) {
            throw new IllegalArgumentException("Documento ya registrado");
        }

        validarDocumento(doc);

        doc.setTransportista(t);

        return repository.save(doc);
    }

    public List<DocumentoPersonal> listarPorTransportista(Long id) {
        return repository.findByTransportistaId(id);
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
}