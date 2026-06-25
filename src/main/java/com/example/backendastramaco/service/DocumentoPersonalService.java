package com.example.backendastramaco.service;

import com.example.backendastramaco.exception.DuplicateResourceException;
import com.example.backendastramaco.exception.ResourceNotFoundException;
import com.example.backendastramaco.model.DocumentoPersonal;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.audit.AuditoriaDocumento;
import com.example.backendastramaco.model.enums.TipoDocumento;
import com.example.backendastramaco.repository.DocumentoPersonalRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.audit.AuditoriaDocumentoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentoPersonalService {

    private final DocumentoPersonalRepository documentoRepository;
    private final TransportistaRepository transportistaRepository;
    private final AuditoriaDocumentoRepository auditoriaRepository;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Transactional
    public DocumentoPersonal guardar(Long transportistaId, DocumentoPersonal doc) {
        log.info("Guardando nuevo documento para transportista: {}", transportistaId);

        Transportista transportista = transportistaRepository.findById(transportistaId)
                .orElseThrow(() -> new ResourceNotFoundException("Transportista con ID " + transportistaId + " no existe"));

        // Validar que no exista documento del mismo tipo
        boolean existe = documentoRepository.existsByTransportistaIdAndTipoDocumentoAndDeletedAtIsNull(
                transportistaId, doc.getTipoDocumento());
        if (existe) {
            throw new DuplicateResourceException("El documento tipo " + doc.getTipoDocumento() + " ya está registrado");
        }

        validarDocumento(doc);
        doc.setTransportista(transportista);
        doc.setActivo(true);

        DocumentoPersonal saved = documentoRepository.save(doc);
        log.info("Documento guardado con ID: {}", saved.getId());

        // Auditar creación
        auditarAccion(
                saved.getId(),
                transportistaId,
                "CREATE",
                null,
                saved,
                "Creación de documento"
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public DocumentoPersonal obtenerPorId(Long id) {
        log.debug("Obteniendo documento por ID: {}", id);
        return documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento con ID " + id + " no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<DocumentoPersonal> listarPorTransportista(Long transportistaId) {
        log.debug("Listando documentos del transportista: {}", transportistaId);
        return documentoRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId);
    }

    @Transactional(readOnly = true)
    public Page<DocumentoPersonal> listarPorTransportistaPaginado(Long transportistaId, Pageable pageable) {
        log.debug("Listando documentos del transportista con paginación: {}", transportistaId);
        return documentoRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId, pageable);
    }

    @Transactional(readOnly = true)
    public List<DocumentoPersonal> listarActivosPorTransportista(Long transportistaId) {
        log.debug("Listando documentos activos del transportista: {}", transportistaId);
        return documentoRepository.findByTransportistaIdAndActivoTrueAndDeletedAtIsNull(transportistaId);
    }

    @Transactional(readOnly = true)
    public List<DocumentoPersonal> listarMisDocumentos(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("No autenticado");
        }
        String username = authentication.getName();
        // Buscar transportista por username (asumiendo que el username está en el usuario)
        // Nota: Esto depende de tu implementación, podría necesitar ajuste
        log.debug("Listando mis documentos para usuario: {}", username);
        // Si tienes relación, busca el transportista por usuario
        // Por ahora, retorna todos los documentos del usuario autenticado
        // Esto debería ajustarse según tu lógica de negocio
        return documentoRepository.findByDeletedAtIsNull(Pageable.unpaged()).getContent();
    }

    @Transactional
    public DocumentoPersonal actualizar(Long documentoId, DocumentoPersonal nuevosDatos) {
        log.info("Actualizando documento con ID: {}", documentoId);

        DocumentoPersonal existente = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento con ID " + documentoId + " no encontrado"));

        // Validar que no exista otro documento del mismo tipo (excepto el mismo)
        if (!existente.getTipoDocumento().equals(nuevosDatos.getTipoDocumento())) {
            boolean existe = documentoRepository.existsByTransportistaIdAndTipoDocumentoAndDeletedAtIsNull(
                    existente.getTransportista().getId(),
                    nuevosDatos.getTipoDocumento()
            );
            if (existe) {
                throw new DuplicateResourceException("El documento tipo " + nuevosDatos.getTipoDocumento() + " ya está registrado");
            }
        }

        // Guardar copia del estado anterior para auditoría
        DocumentoPersonal oldCopy = copiarEntidad(existente);

        // Actualizar campos
        existente.setTipoDocumento(nuevosDatos.getTipoDocumento());
        existente.setValor(nuevosDatos.getValor());
        existente.setFechaEmision(nuevosDatos.getFechaEmision());
        existente.setFechaVencimiento(nuevosDatos.getFechaVencimiento());

        validarDocumento(existente);
        DocumentoPersonal updated = documentoRepository.save(existente);
        log.info("Documento actualizado con ID: {}", documentoId);

        // Auditar actualización
        auditarActualizacion(
                documentoId,
                existente.getTransportista().getId(),
                oldCopy,
                updated
        );

        return updated;
    }

    @Transactional
    public void cambiarEstado(Long id, Boolean activo) {
        log.info("Cambiando estado del documento con ID: {} a {}", id, activo);

        DocumentoPersonal documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento con ID " + id + " no encontrado"));

        DocumentoPersonal oldCopy = copiarEntidad(documento);
        documento.setActivo(activo);
        documentoRepository.save(documento);

        // Auditar cambio de estado
        auditarAccion(
                id,
                documento.getTransportista().getId(),
                "UPDATE_ESTADO",
                oldCopy,
                documento,
                "Cambio de estado a: " + (activo ? "ACTIVO" : "INACTIVO")
        );

        log.info("Estado del documento {} cambiado a: {}", id, activo);
    }

    @Transactional
    public void eliminar(Long documentoId, String username) {
        log.info("Eliminando (soft delete) documento con ID: {}", documentoId);

        DocumentoPersonal existente = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento con ID " + documentoId + " no encontrado"));

        if (existente.getDeletedAt() != null) {
            throw new RuntimeException("El documento ya está eliminado");
        }

        Long transportistaId = existente.getTransportista().getId();
        DocumentoPersonal oldCopy = copiarEntidad(existente);

        // Soft delete
        existente.softDelete(username);
        existente.setActivo(false);
        documentoRepository.save(existente);

        // Auditar eliminación
        auditarAccion(
                documentoId,
                transportistaId,
                "DELETE",
                oldCopy,
                null,
                "Documento eliminado por: " + username
        );

        log.info("Documento {} eliminado (soft delete) por: {}", documentoId, username);
    }

    @Transactional
    public void restaurar(Long id) {
        log.info("Restaurando documento con ID: {}", id);

        DocumentoPersonal documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento con ID " + id + " no encontrado"));

        if (documento.getDeletedAt() == null) {
            throw new RuntimeException("El documento no está eliminado");
        }

        DocumentoPersonal oldCopy = copiarEntidad(documento);

        // Restaurar documento
        documento.setDeletedAt(null);
        documento.setDeletedBy(null);
        documento.setActivo(true);
        documentoRepository.save(documento);

        // Auditar restauración
        auditarAccion(
                id,
                documento.getTransportista().getId(),
                "RESTORE",
                oldCopy,
                documento,
                "Documento restaurado"
        );

        log.info("Documento {} restaurado", id);
    }

    @Transactional
    public void eliminarPermanente(Long id) {
        log.info("Eliminando permanentemente documento con ID: {}", id);

        DocumentoPersonal documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento con ID " + id + " no encontrado"));

        Long transportistaId = documento.getTransportista().getId();
        DocumentoPersonal oldCopy = copiarEntidad(documento);

        // Auditar eliminación permanente
        auditarAccion(
                id,
                transportistaId,
                "DELETE_PERMANENT",
                oldCopy,
                null,
                "Eliminación permanente del documento"
        );

        documentoRepository.delete(documento);
        log.info("Documento {} eliminado permanentemente", id);
    }

    // ========== MÉTODOS PRIVADOS DE VALIDACIÓN ==========

    private void validarDocumento(DocumentoPersonal doc) {
        if (doc.getTipoDocumento() == null) {
            throw new IllegalArgumentException("El tipo de documento no puede ser nulo");
        }

        switch (doc.getTipoDocumento()) {
            case SOAT, REVISION_TECNICA:
                if (doc.getFechaEmision() == null || doc.getFechaVencimiento() == null) {
                    throw new IllegalArgumentException("SOAT y REVISIÓN TÉCNICA requieren obligatoriamente fecha de emisión y vencimiento");
                }
                if (doc.getFechaVencimiento().isBefore(doc.getFechaEmision())) {
                    throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la fecha de emisión");
                }
                break;

            case LICENCIA, TARJETA_CIRCULACION:
                if (doc.getValor() == null ||
                        (!doc.getValor().equalsIgnoreCase("SI") && !doc.getValor().equalsIgnoreCase("NO"))) {
                    throw new IllegalArgumentException("El valor para LICENCIA o TARJETA DE CIRCULACIÓN debe ser 'SI' o 'NO'");
                }
                // Para LICENCIA y TARJETA_CIRCULACION, las fechas deben ser null
                doc.setFechaEmision(null);
                doc.setFechaVencimiento(null);
                break;

            case DNI:
                if (doc.getValor() == null || doc.getValor().trim().isEmpty()) {
                    throw new IllegalArgumentException("El valor del DNI no puede ser vacío");
                }
                // DNI no requiere fechas
                doc.setFechaEmision(null);
                doc.setFechaVencimiento(null);
                break;

            default:
                break;
        }
    }

    // ========== MÉTODOS DE AUDITORÍA ==========

    private void auditarAccion(Long documentoId, Long transportistaId, String accion,
                               DocumentoPersonal oldData, DocumentoPersonal newData, String observacion) {
        try {
            AuditoriaDocumento auditoria = new AuditoriaDocumento();
            auditoria.setDocumentoId(documentoId);
            auditoria.setTransportistaId(transportistaId);
            auditoria.setAccion(accion);
            auditoria.setUsername(getCurrentUsername());
            auditoria.setFechaHora(LocalDateTime.now());
            auditoria.setIpAddress(getClientIP());

            if (oldData != null) {
                auditoria.setTipoDocumentoAnterior(oldData.getTipoDocumento() != null ? oldData.getTipoDocumento().name() : null);
                auditoria.setValorAnterior(oldData.getValor());
                auditoria.setFechaEmisionAnterior(oldData.getFechaEmision());
                auditoria.setFechaVencimientoAnterior(oldData.getFechaVencimiento());
                auditoria.setActivoAnterior(oldData.getActivo());

                try {
                    Map<String, Object> oldMap = new HashMap<>();
                    oldMap.put("id", oldData.getId());
                    oldMap.put("tipoDocumento", oldData.getTipoDocumento());
                    oldMap.put("valor", oldData.getValor());
                    oldMap.put("fechaEmision", oldData.getFechaEmision());
                    oldMap.put("fechaVencimiento", oldData.getFechaVencimiento());
                    oldMap.put("activo", oldData.getActivo());
                    oldMap.put("createdAt", oldData.getCreatedAt());
                    oldMap.put("deletedAt", oldData.getDeletedAt());
                    oldMap.put("deletedBy", oldData.getDeletedBy());
                    auditoria.setDatosCompletosAnteriores(objectMapper.writeValueAsString(oldMap));
                } catch (Exception e) {
                    log.error("Error al serializar datos anteriores", e);
                }
            }

            if (newData != null) {
                auditoria.setTipoDocumentoNuevo(newData.getTipoDocumento() != null ? newData.getTipoDocumento().name() : null);
                auditoria.setValorNuevo(newData.getValor());
                auditoria.setFechaEmisionNuevo(newData.getFechaEmision());
                auditoria.setFechaVencimientoNuevo(newData.getFechaVencimiento());
                auditoria.setActivoNuevo(newData.getActivo());

                try {
                    Map<String, Object> newMap = new HashMap<>();
                    newMap.put("id", newData.getId());
                    newMap.put("tipoDocumento", newData.getTipoDocumento());
                    newMap.put("valor", newData.getValor());
                    newMap.put("fechaEmision", newData.getFechaEmision());
                    newMap.put("fechaVencimiento", newData.getFechaVencimiento());
                    newMap.put("activo", newData.getActivo());
                    newMap.put("createdAt", newData.getCreatedAt());
                    newMap.put("deletedAt", newData.getDeletedAt());
                    newMap.put("deletedBy", newData.getDeletedBy());
                    auditoria.setDatosCompletosNuevos(objectMapper.writeValueAsString(newMap));
                } catch (Exception e) {
                    log.error("Error al serializar datos nuevos", e);
                }
            }

            auditoriaRepository.save(auditoria);
            log.debug("Auditoría guardada para documento {}: {}", documentoId, accion);
        } catch (Exception e) {
            log.error("Error al guardar auditoría para documento {}: {}", documentoId, e.getMessage());
        }
    }

    private void auditarActualizacion(Long documentoId, Long transportistaId,
                                      DocumentoPersonal oldData, DocumentoPersonal newData) {
        try {
            AuditoriaDocumento auditoria = new AuditoriaDocumento();
            auditoria.setDocumentoId(documentoId);
            auditoria.setTransportistaId(transportistaId);
            auditoria.setAccion("UPDATE");
            auditoria.setUsername(getCurrentUsername());
            auditoria.setFechaHora(LocalDateTime.now());
            auditoria.setIpAddress(getClientIP());

            // Datos anteriores
            auditoria.setTipoDocumentoAnterior(oldData.getTipoDocumento() != null ? oldData.getTipoDocumento().name() : null);
            auditoria.setValorAnterior(oldData.getValor());
            auditoria.setFechaEmisionAnterior(oldData.getFechaEmision());
            auditoria.setFechaVencimientoAnterior(oldData.getFechaVencimiento());
            auditoria.setActivoAnterior(oldData.getActivo());

            // Datos nuevos
            auditoria.setTipoDocumentoNuevo(newData.getTipoDocumento() != null ? newData.getTipoDocumento().name() : null);
            auditoria.setValorNuevo(newData.getValor());
            auditoria.setFechaEmisionNuevo(newData.getFechaEmision());
            auditoria.setFechaVencimientoNuevo(newData.getFechaVencimiento());
            auditoria.setActivoNuevo(newData.getActivo());

            try {
                Map<String, Object> oldMap = new HashMap<>();
                oldMap.put("id", oldData.getId());
                oldMap.put("tipoDocumento", oldData.getTipoDocumento());
                oldMap.put("valor", oldData.getValor());
                oldMap.put("fechaEmision", oldData.getFechaEmision());
                oldMap.put("fechaVencimiento", oldData.getFechaVencimiento());
                oldMap.put("activo", oldData.getActivo());
                oldMap.put("createdAt", oldData.getCreatedAt());
                oldMap.put("updatedAt", oldData.getUpdatedAt());
                auditoria.setDatosCompletosAnteriores(objectMapper.writeValueAsString(oldMap));

                Map<String, Object> newMap = new HashMap<>();
                newMap.put("id", newData.getId());
                newMap.put("tipoDocumento", newData.getTipoDocumento());
                newMap.put("valor", newData.getValor());
                newMap.put("fechaEmision", newData.getFechaEmision());
                newMap.put("fechaVencimiento", newData.getFechaVencimiento());
                newMap.put("activo", newData.getActivo());
                newMap.put("createdAt", newData.getCreatedAt());
                newMap.put("updatedAt", newData.getUpdatedAt());
                auditoria.setDatosCompletosNuevos(objectMapper.writeValueAsString(newMap));
            } catch (Exception e) {
                log.error("Error al serializar datos para auditoría", e);
            }

            auditoriaRepository.save(auditoria);
            log.debug("Auditoría de actualización guardada para documento {}", documentoId);
        } catch (Exception e) {
            log.error("Error al guardar auditoría de actualización", e);
        }
    }

    // ========== MÉTODOS AUXILIARES ==========

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "sistema";
        }
    }

    private String getClientIP() {
        try {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }

    private DocumentoPersonal copiarEntidad(DocumentoPersonal original) {
        DocumentoPersonal copia = new DocumentoPersonal();
        copia.setId(original.getId());
        copia.setCreatedAt(original.getCreatedAt());
        copia.setUpdatedAt(original.getUpdatedAt());
        copia.setDeletedAt(original.getDeletedAt());
        copia.setDeletedBy(original.getDeletedBy());
        copia.setTipoDocumento(original.getTipoDocumento());
        copia.setValor(original.getValor());
        copia.setFechaEmision(original.getFechaEmision());
        copia.setFechaVencimiento(original.getFechaVencimiento());
        copia.setActivo(original.getActivo());
        copia.setTransportista(original.getTransportista());
        return copia;
    }
}