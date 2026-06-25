package com.example.backendastramaco.service;

import com.example.backendastramaco.dto.AumentarCargaRequestDTO;
import com.example.backendastramaco.dto.CargaRequestDTO;
import com.example.backendastramaco.dto.CargaResponseDTO;
import com.example.backendastramaco.exception.DuplicateResourceException;
import com.example.backendastramaco.exception.ResourceNotFoundException;
import com.example.backendastramaco.model.Carga;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.audit.AuditoriaCarga;
import com.example.backendastramaco.model.enums.TipoMaterial;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.CargaRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.audit.AuditoriaCargaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class CargaService {

    private final CargaRepository cargaRepository;
    private final TransportistaRepository transportistaRepository;
    private final AuditoriaCargaRepository auditoriaRepository;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Transactional
    public CargaResponseDTO crearCarga(Long transportistaId, CargaRequestDTO requestDTO) {
        log.info("Creando carga para transportista: {}", transportistaId);

        Transportista transportista = obtenerCamioneroValido(transportistaId);
        validarMaterialCamionero(requestDTO.getTipoMaterial());

        // Verificar si ya existe carga para este transportista
        if (cargaRepository.existsByTransportistaIdAndDeletedAtIsNull(transportistaId)) {
            throw new DuplicateResourceException("El transportista ya tiene una carga registrada");
        }

        Carga carga = Carga.builder()
                .transportista(transportista)
                .tipoMaterial(requestDTO.getTipoMaterial())
                .cantidadDisponible(requestDTO.getCantidadDisponible())
                .build();

        Carga saved = cargaRepository.save(carga);
        log.info("Carga creada con ID: {}", saved.getId());

        // Auditar creación
        auditarAccion(
                saved.getId(),
                transportistaId,
                "CREATE",
                null,
                saved,
                "Creación de carga"
        );

        return toResponseDTO(saved);
    }

    @Transactional
    public CargaResponseDTO actualizarCarga(Long transportistaId, CargaRequestDTO requestDTO) {
        log.info("Actualizando carga para transportista: {}", transportistaId);

        Transportista transportista = obtenerCamioneroValido(transportistaId);
        validarMaterialCamionero(requestDTO.getTipoMaterial());

        Carga carga = cargaRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId)
                .orElseThrow(() -> new ResourceNotFoundException("El transportista no tiene carga registrada"));

        // Guardar copia del estado anterior para auditoría
        Carga oldCopy = copiarEntidad(carga);

        // Actualizar campos
        carga.setTipoMaterial(requestDTO.getTipoMaterial());
        carga.setCantidadDisponible(requestDTO.getCantidadDisponible());

        Carga saved = cargaRepository.save(carga);
        log.info("Carga actualizada con ID: {}", saved.getId());

        // Auditar actualización
        auditarActualizacion(oldCopy, saved);

        return toResponseDTO(saved);
    }

    @Transactional
    public CargaResponseDTO aumentarCarga(Long transportistaId, AumentarCargaRequestDTO requestDTO) {
        log.info("Aumentando carga para transportista: {}", transportistaId);

        Transportista transportista = obtenerCamioneroValido(transportistaId);
        validarMaterialCamionero(requestDTO.getTipoMaterial());

        Carga carga = cargaRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId)
                .orElseThrow(() -> new ResourceNotFoundException(CARGA_NO_REGISTRADA));

        if (!carga.getTipoMaterial().equals(requestDTO.getTipoMaterial())) {
            throw new IllegalArgumentException(MATERIAL_DISTINTO);
        }

        if (requestDTO.getCantidadAgregar() <= 0) {
            throw new IllegalArgumentException("La cantidad a agregar debe ser mayor a cero");
        }

        Carga oldCopy = copiarEntidad(carga);

        carga.setCantidadDisponible(carga.getCantidadDisponible() + requestDTO.getCantidadAgregar());

        Carga saved = cargaRepository.save(carga);
        log.info("Carga aumentada con ID: {}, nueva cantidad: {}", saved.getId(), saved.getCantidadDisponible());

        // Auditar aumento de carga
        auditarAccion(
                saved.getId(),
                transportistaId,
                "UPDATE_AUMENTO",
                oldCopy,
                saved,
                "Aumento de carga en: " + requestDTO.getCantidadAgregar()
        );

        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public CargaResponseDTO obtenerCarga(Long transportistaId) {
        log.debug("Obteniendo carga del transportista: {}", transportistaId);

        obtenerCamioneroValido(transportistaId);

        return cargaRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("El transportista aún no tiene carga registrada"));
    }

    @Transactional(readOnly = true)
    public CargaResponseDTO obtenerPorId(Long id) {
        log.debug("Obteniendo carga por ID: {}", id);

        Carga carga = cargaRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carga no encontrada con ID: " + id));

        return toResponseDTO(carga);
    }

    @Transactional(readOnly = true)
    public Page<CargaResponseDTO> listarTodas(Pageable pageable, String tipoMaterial) {
        log.debug("Listando todas las cargas con filtro: {}", tipoMaterial);

        if (tipoMaterial != null && !tipoMaterial.isEmpty()) {
            return cargaRepository.findByTipoMaterialAndDeletedAtIsNull(
                    TipoMaterial.valueOf(tipoMaterial.toUpperCase()),
                    pageable).map(this::toResponseDTO);
        }
        return cargaRepository.findByDeletedAtIsNull(pageable)
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public Page<CargaResponseDTO> listarHistorialPorTransportista(Long transportistaId, Pageable pageable) {
        log.debug("Listando historial de cargas del transportista: {}", transportistaId);

        // Verificar que el transportista existe
        if (!transportistaRepository.existsById(transportistaId)) {
            throw new ResourceNotFoundException("Transportista no encontrado con ID: " + transportistaId);
        }

        return cargaRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId, pageable)
                .map(this::toResponseDTO);
    }

    @Transactional
    public void eliminar(Long id, String username) {
        log.info("Eliminando (soft delete) carga con ID: {}", id);

        Carga existente = cargaRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carga no encontrada con ID: " + id));

        Long transportistaId = existente.getTransportista().getId();
        Carga oldCopy = copiarEntidad(existente);

        // Soft delete
        existente.softDelete(username);
        cargaRepository.save(existente);

        // Auditar eliminación
        auditarAccion(
                id,
                transportistaId,
                "DELETE",
                oldCopy,
                null,
                "Carga eliminada por: " + username
        );

        log.info("Carga {} eliminada (soft delete) por: {}", id, username);
    }

    @Transactional
    public void restaurar(Long id) {
        log.info("Restaurando carga con ID: {}", id);

        Carga carga = cargaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carga no encontrada con ID: " + id));

        if (carga.getDeletedAt() == null) {
            throw new RuntimeException("La carga no está eliminada");
        }

        Carga oldCopy = copiarEntidad(carga);

        // Restaurar carga
        carga.setDeletedAt(null);
        carga.setDeletedBy(null);
        cargaRepository.save(carga);

        // Auditar restauración
        auditarAccion(
                id,
                carga.getTransportista().getId(),
                "RESTORE",
                oldCopy,
                carga,
                "Carga restaurada"
        );

        log.info("Carga {} restaurada", id);
    }

    @Transactional
    public void eliminarPermanente(Long id) {
        log.info("Eliminando permanentemente carga con ID: {}", id);

        Carga carga = cargaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carga no encontrada con ID: " + id));

        Long transportistaId = carga.getTransportista().getId();
        Carga oldCopy = copiarEntidad(carga);

        // Auditar eliminación permanente
        auditarAccion(
                id,
                transportistaId,
                "DELETE_PERMANENT",
                oldCopy,
                null,
                "Eliminación permanente de la carga"
        );

        cargaRepository.delete(carga);
        log.info("Carga {} eliminada permanentemente", id);
    }

    // ========== MÉTODOS PRIVADOS DE VALIDACIÓN ==========

    private Transportista obtenerCamioneroValido(Long transportistaId) {
        Transportista transportista = transportistaRepository.findById(transportistaId)
                .orElseThrow(() -> new ResourceNotFoundException("Transportista con ID " + transportistaId + " no existe"));

        if (transportista.getTipoTransporte() != TipoTransporte.CAMIONERO) {
            throw new IllegalArgumentException("Solo los transportistas CAMIONERO pueden manejar carga");
        }

        return transportista;
    }

    private void validarMaterialCamionero(TipoMaterial tipoMaterial) {
        if (tipoMaterial != TipoMaterial.PANDERETA && tipoMaterial != TipoMaterial.TECHO) {
            throw new IllegalArgumentException("El transportista CAMIONERO solo puede registrar PANDERETA o TECHO");
        }
    }

    // ========== MÉTODOS DE AUDITORÍA ==========

    private void auditarAccion(Long cargaId, Long transportistaId, String accion,
                               Carga oldData, Carga newData, String observacion) {
        try {
            AuditoriaCarga auditoria = new AuditoriaCarga();
            auditoria.setCargaId(cargaId);
            auditoria.setTransportistaId(transportistaId);
            auditoria.setAccion(accion);
            auditoria.setUsername(getCurrentUsername());
            auditoria.setFechaHora(LocalDateTime.now());
            auditoria.setIpAddress(getClientIP());

            if (oldData != null) {
                auditoria.setTipoMaterialAnterior(oldData.getTipoMaterial() != null ? oldData.getTipoMaterial().name() : null);
                auditoria.setCantidadAnterior(oldData.getCantidadDisponible());

                try {
                    Map<String, Object> oldMap = new HashMap<>();
                    oldMap.put("id", oldData.getId());
                    oldMap.put("tipoMaterial", oldData.getTipoMaterial());
                    oldMap.put("cantidadDisponible", oldData.getCantidadDisponible());
                    oldMap.put("createdAt", oldData.getCreatedAt());
                    oldMap.put("deletedAt", oldData.getDeletedAt());
                    oldMap.put("deletedBy", oldData.getDeletedBy());
                    auditoria.setDatosCompletosAnteriores(objectMapper.writeValueAsString(oldMap));
                } catch (Exception e) {
                    log.error("Error al serializar datos anteriores", e);
                }
            }

            if (newData != null) {
                auditoria.setTipoMaterialNuevo(newData.getTipoMaterial() != null ? newData.getTipoMaterial().name() : null);
                auditoria.setCantidadNuevo(newData.getCantidadDisponible());

                try {
                    Map<String, Object> newMap = new HashMap<>();
                    newMap.put("id", newData.getId());
                    newMap.put("tipoMaterial", newData.getTipoMaterial());
                    newMap.put("cantidadDisponible", newData.getCantidadDisponible());
                    newMap.put("createdAt", newData.getCreatedAt());
                    newMap.put("deletedAt", newData.getDeletedAt());
                    newMap.put("deletedBy", newData.getDeletedBy());
                    auditoria.setDatosCompletosNuevos(objectMapper.writeValueAsString(newMap));
                } catch (Exception e) {
                    log.error("Error al serializar datos nuevos", e);
                }
            }

            auditoriaRepository.save(auditoria);
            log.debug("Auditoría guardada para carga {}: {}", cargaId, accion);
        } catch (Exception e) {
            log.error("Error al guardar auditoría para carga {}: {}", cargaId, e.getMessage());
        }
    }

    private void auditarActualizacion(Carga oldData, Carga newData) {
        try {
            AuditoriaCarga auditoria = new AuditoriaCarga();
            auditoria.setCargaId(newData.getId());
            auditoria.setTransportistaId(newData.getTransportista().getId());
            auditoria.setAccion("UPDATE");
            auditoria.setUsername(getCurrentUsername());
            auditoria.setFechaHora(LocalDateTime.now());
            auditoria.setIpAddress(getClientIP());

            // Datos anteriores
            auditoria.setTipoMaterialAnterior(oldData.getTipoMaterial() != null ? oldData.getTipoMaterial().name() : null);
            auditoria.setCantidadAnterior(oldData.getCantidadDisponible());

            // Datos nuevos
            auditoria.setTipoMaterialNuevo(newData.getTipoMaterial() != null ? newData.getTipoMaterial().name() : null);
            auditoria.setCantidadNuevo(newData.getCantidadDisponible());

            try {
                Map<String, Object> oldMap = new HashMap<>();
                oldMap.put("id", oldData.getId());
                oldMap.put("tipoMaterial", oldData.getTipoMaterial());
                oldMap.put("cantidadDisponible", oldData.getCantidadDisponible());
                oldMap.put("createdAt", oldData.getCreatedAt());
                oldMap.put("updatedAt", oldData.getUpdatedAt());
                auditoria.setDatosCompletosAnteriores(objectMapper.writeValueAsString(oldMap));

                Map<String, Object> newMap = new HashMap<>();
                newMap.put("id", newData.getId());
                newMap.put("tipoMaterial", newData.getTipoMaterial());
                newMap.put("cantidadDisponible", newData.getCantidadDisponible());
                newMap.put("createdAt", newData.getCreatedAt());
                newMap.put("updatedAt", newData.getUpdatedAt());
                auditoria.setDatosCompletosNuevos(objectMapper.writeValueAsString(newMap));
            } catch (Exception e) {
                log.error("Error al serializar datos para auditoría", e);
            }

            auditoriaRepository.save(auditoria);
            log.debug("Auditoría de actualización guardada para carga {}", newData.getId());
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

    private CargaResponseDTO toResponseDTO(Carga carga) {
        Transportista transportista = carga.getTransportista();

        return CargaResponseDTO.builder()
                .id(carga.getId())
                .transportistaId(transportista.getId())
                .transportistaNombre((transportista.getNombre() + " " + transportista.getApellidos()).trim())
                .tipoMaterial(carga.getTipoMaterial())
                .cantidadDisponible(carga.getCantidadDisponible())
                .build();
    }

    private Carga copiarEntidad(Carga original) {
        Carga copia = new Carga();
        copia.setId(original.getId());
        copia.setCreatedAt(original.getCreatedAt());
        copia.setUpdatedAt(original.getUpdatedAt());
        copia.setDeletedAt(original.getDeletedAt());
        copia.setDeletedBy(original.getDeletedBy());
        copia.setTransportista(original.getTransportista());
        copia.setTipoMaterial(original.getTipoMaterial());
        copia.setCantidadDisponible(original.getCantidadDisponible());
        return copia;
    }

    // Constantes
    private static final String CARGA_NO_REGISTRADA = "El transportista no tiene carga registrada";
    private static final String MATERIAL_DISTINTO = "Solo se puede aumentar si el material es el mismo que la carga actual";
}