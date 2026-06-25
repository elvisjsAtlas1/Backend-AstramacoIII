package com.example.backendastramaco.service;

import com.example.backendastramaco.dto.PedidoRequestDTO;
import com.example.backendastramaco.dto.PedidoResponseDTO;
import com.example.backendastramaco.exception.DuplicateResourceException;
import com.example.backendastramaco.exception.ResourceNotFoundException;
import com.example.backendastramaco.model.Carga;
import com.example.backendastramaco.model.Pedido;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.audit.AuditoriaPedido;
import com.example.backendastramaco.model.enums.EstadoPedido;
import com.example.backendastramaco.model.enums.TipoMaterial;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.CargaRepository;
import com.example.backendastramaco.repository.PedidoRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.audit.AuditoriaPedidoRepository;
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
public class PedidoService {

    private static final String CODIGO_VERIFICACION_POR_DEFECTO = "1234";

    private final PedidoRepository pedidoRepository;
    private final TransportistaRepository transportistaRepository;
    private final CargaRepository cargaRepository;
    private final AuditoriaPedidoRepository auditoriaRepository;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Transactional
    public PedidoResponseDTO crearPedido(PedidoRequestDTO dto) {
        log.info("Creando nuevo pedido para transportista: {}", dto.getTransportistaId());

        Transportista transportista = transportistaRepository.findById(dto.getTransportistaId())
                .orElseThrow(() -> new ResourceNotFoundException("Transportista no encontrado con ID: " + dto.getTransportistaId()));

        validarTipoTransporte(dto, transportista);
        procesarCargaSiEsCamionero(dto, transportista);

        Pedido pedido = Pedido.builder()
                .clienteNombre(dto.getClienteNombre())
                .clienteTelefono(dto.getClienteTelefono())
                .direccionEnvio(dto.getDireccionEnvio())
                .tipoTransporte(dto.getTipoTransporte())
                .material(dto.getMaterial())
                .cantidad(dto.getCantidad())
                .montoTotal(dto.getMontoTotal())
                .adelanto(dto.getAdelanto())
                .piso(dto.getPiso())
                .horaEnvio(dto.getHoraEnvio())
                .transportista(transportista)
                .codigoVerificacion(CODIGO_VERIFICACION_POR_DEFECTO)
                .build();

        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        log.info("Pedido creado con ID: {}", pedidoGuardado.getId());

        // Auditar creación
        auditarAccion(
                pedidoGuardado.getId(),
                "CREATE",
                null,
                pedidoGuardado,
                "Creación de pedido"
        );

        return toResponseDTO(pedidoGuardado);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listar(Pageable pageable, String estado, Long transportistaId) {
        log.debug("Listando pedidos con filtros - estado: {}, transportistaId: {}", estado, transportistaId);

        if (estado != null && transportistaId != null) {
            return pedidoRepository.findByEstadoAndTransportistaIdAndDeletedAtIsNull(
                    EstadoPedido.valueOf(estado.toUpperCase()),
                    transportistaId,
                    pageable).map(this::toResponseDTO);
        } else if (estado != null) {
            return pedidoRepository.findByEstadoAndDeletedAtIsNull(
                    EstadoPedido.valueOf(estado.toUpperCase()),
                    pageable).map(this::toResponseDTO);
        } else if (transportistaId != null) {
            return pedidoRepository.findByTransportistaIdAndDeletedAtIsNull(
                    transportistaId,
                    pageable).map(this::toResponseDTO);
        } else {
            return pedidoRepository.findByDeletedAtIsNull(pageable)
                    .map(this::toResponseDTO);
        }
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarTodos(Pageable pageable) {
        log.debug("Listando todos los pedidos (incluyendo eliminados)");
        return pedidoRepository.findAll(pageable).map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarEliminados(Pageable pageable) {
        log.debug("Listando pedidos eliminados");
        return pedidoRepository.findByDeletedAtIsNotNull(pageable)
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public PedidoResponseDTO obtenerPorId(Long id) {
        log.debug("Obteniendo pedido por ID: {}", id);
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + id));
        return toResponseDTO(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPorTransportista(Long transportistaId) {
        log.debug("Listando pedidos por transportista: {}", transportistaId);
        return pedidoRepository.findByTransportistaIdAndDeletedAtIsNullOrderByHoraEnvioDesc(transportistaId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarPorTransportista(Long transportistaId, Pageable pageable, String estado) {
        log.debug("Listando pedidos por transportista: {}, estado: {}", transportistaId, estado);

        if (estado != null) {
            return pedidoRepository.findByTransportistaIdAndEstadoAndDeletedAtIsNull(
                    transportistaId,
                    EstadoPedido.valueOf(estado.toUpperCase()),
                    pageable).map(this::toResponseDTO);
        }
        return pedidoRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId, pageable)
                .map(this::toResponseDTO);
    }

    @Transactional
    public PedidoResponseDTO actualizar(Long id, PedidoRequestDTO dto) {
        log.info("Actualizando pedido con ID: {}", id);

        Pedido existente = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + id));

        Transportista transportista = transportistaRepository.findById(dto.getTransportistaId())
                .orElseThrow(() -> new ResourceNotFoundException("Transportista no encontrado con ID: " + dto.getTransportistaId()));

        validarTipoTransporte(dto, transportista);

        // Guardar copia del estado anterior para auditoría
        Pedido oldCopy = copiarEntidad(existente);

        // Actualizar campos
        existente.setClienteNombre(dto.getClienteNombre());
        existente.setClienteTelefono(dto.getClienteTelefono());
        existente.setDireccionEnvio(dto.getDireccionEnvio());
        existente.setTipoTransporte(dto.getTipoTransporte());
        existente.setMaterial(dto.getMaterial());
        existente.setCantidad(dto.getCantidad());
        existente.setMontoTotal(dto.getMontoTotal());
        existente.setAdelanto(dto.getAdelanto());
        existente.setPiso(dto.getPiso());
        existente.setHoraEnvio(dto.getHoraEnvio());
        existente.setTransportista(transportista);

        Pedido updated = pedidoRepository.save(existente);
        log.info("Pedido actualizado con ID: {}", id);

        // Auditar actualización
        auditarActualizacion(oldCopy, updated);

        return toResponseDTO(updated);
    }

    @Transactional
    public PedidoResponseDTO cambiarEstado(Long id, String nuevoEstado) {
        log.info("Cambiando estado del pedido con ID: {} a {}", id, nuevoEstado);

        Pedido existente = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + id));

        // Guardar copia del estado anterior
        Pedido oldCopy = copiarEntidad(existente);

        // Actualizar estado
        existente.setEstado(EstadoPedido.valueOf(nuevoEstado.toUpperCase()));

        Pedido updated = pedidoRepository.save(existente);
        log.info("Estado del pedido {} cambiado a: {}", id, nuevoEstado);

        // Auditar cambio de estado
        auditarAccion(
                id,
                "UPDATE_ESTADO",
                oldCopy,
                updated,
                "Cambio de estado a: " + nuevoEstado
        );

        return toResponseDTO(updated);
    }

    @Transactional
    public void eliminar(Long id, String username) {
        log.info("Eliminando (soft delete) pedido con ID: {}", id);

        Pedido existente = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + id));

        if (existente.getDeletedAt() != null) {
            throw new RuntimeException("El pedido ya está eliminado");
        }

        // Guardar copia para auditoría
        Pedido oldCopy = copiarEntidad(existente);

        // Soft delete
        existente.softDelete(username);
        pedidoRepository.save(existente);

        // Auditar eliminación
        auditarAccion(
                id,
                "DELETE",
                oldCopy,
                null,
                "Pedido eliminado por: " + username
        );

        log.info("Pedido {} eliminado (soft delete) por: {}", id, username);
    }

    @Transactional
    public void restaurar(Long id) {
        log.info("Restaurando pedido con ID: {}", id);

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + id));

        if (pedido.getDeletedAt() == null) {
            throw new RuntimeException("El pedido no está eliminado");
        }

        Pedido oldCopy = copiarEntidad(pedido);

        // Restaurar pedido
        pedido.setDeletedAt(null);
        pedido.setDeletedBy(null);
        pedidoRepository.save(pedido);

        // Auditar restauración
        auditarAccion(
                id,
                "RESTORE",
                oldCopy,
                pedido,
                "Pedido restaurado"
        );

        log.info("Pedido {} restaurado", id);
    }

    @Transactional
    public void eliminarPermanente(Long id) {
        log.info("Eliminando permanentemente pedido con ID: {}", id);

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + id));

        // Guardar copia para auditoría antes de eliminar
        Pedido oldCopy = copiarEntidad(pedido);

        // Auditar eliminación permanente
        auditarAccion(
                id,
                "DELETE_PERMANENT",
                oldCopy,
                null,
                "Eliminación permanente del pedido"
        );

        pedidoRepository.delete(pedido);
        log.info("Pedido {} eliminado permanentemente", id);
    }

    // ========== MÉTODOS PRIVADOS DE VALIDACIÓN ==========

    private void validarTipoTransporte(PedidoRequestDTO dto, Transportista transportista) {
        if (!transportista.getTipoTransporte().equals(dto.getTipoTransporte())) {
            throw new IllegalArgumentException("El tipo de transporte del pedido no coincide con el transportista seleccionado");
        }
    }

    private void procesarCargaSiEsCamionero(PedidoRequestDTO dto, Transportista transportista) {
        if (transportista.getTipoTransporte() != TipoTransporte.CAMIONERO) {
            return;
        }

        validarMaterialCamionero(dto.getMaterial());

        Carga carga = cargaRepository
                .findByTransportistaId(transportista.getId())
                .orElseThrow(() -> new RuntimeException("El transportista no tiene carga registrada"));

        if (!carga.getTipoMaterial().equals(dto.getMaterial())) {
            throw new IllegalArgumentException("El transportista no cuenta con ese material en su carga actual");
        }

        if (carga.getCantidadDisponible() < dto.getCantidad()) {
            throw new IllegalArgumentException("Stock insuficiente para atender el pedido");
        }

        carga.setCantidadDisponible(carga.getCantidadDisponible() - dto.getCantidad());
        cargaRepository.save(carga);
    }

    private void validarMaterialCamionero(TipoMaterial material) {
        if (material != TipoMaterial.PANDERETA && material != TipoMaterial.TECHO) {
            throw new IllegalArgumentException("El transportista camionero solo puede trabajar con materiales PANDERETA o TECHO");
        }
    }

    // ========== MÉTODOS DE AUDITORÍA ==========

    private void auditarAccion(Long pedidoId, String accion, Pedido oldData, Pedido newData, String observacion) {
        try {
            AuditoriaPedido auditoria = new AuditoriaPedido();
            auditoria.setPedidoId(pedidoId);
            auditoria.setAccion(accion);
            auditoria.setUsername(getCurrentUsername());
            auditoria.setFechaHora(LocalDateTime.now());
            auditoria.setIpAddress(getClientIP());

            if (oldData != null) {
                auditoria.setClienteNombreAnterior(oldData.getClienteNombre());
                auditoria.setClienteTelefonoAnterior(oldData.getClienteTelefono());
                auditoria.setDireccionEnvioAnterior(oldData.getDireccionEnvio());
                auditoria.setTipoTransporteAnterior(oldData.getTipoTransporte() != null ? oldData.getTipoTransporte().name() : null);
                auditoria.setMaterialAnterior(oldData.getMaterial() != null ? oldData.getMaterial().name() : null);
                auditoria.setCantidadAnterior(oldData.getCantidad());
                auditoria.setMontoTotalAnterior(oldData.getMontoTotal());
                auditoria.setAdelantoAnterior(oldData.getAdelanto());
                auditoria.setEstadoAnterior(oldData.getEstado() != null ? oldData.getEstado().name() : null);

                try {
                    Map<String, Object> oldMap = new HashMap<>();
                    oldMap.put("id", oldData.getId());
                    oldMap.put("clienteNombre", oldData.getClienteNombre());
                    oldMap.put("clienteTelefono", oldData.getClienteTelefono());
                    oldMap.put("direccionEnvio", oldData.getDireccionEnvio());
                    oldMap.put("tipoTransporte", oldData.getTipoTransporte());
                    oldMap.put("material", oldData.getMaterial());
                    oldMap.put("cantidad", oldData.getCantidad());
                    oldMap.put("montoTotal", oldData.getMontoTotal());
                    oldMap.put("adelanto", oldData.getAdelanto());
                    oldMap.put("estado", oldData.getEstado());
                    oldMap.put("createdAt", oldData.getCreatedAt());
                    oldMap.put("deletedAt", oldData.getDeletedAt());
                    oldMap.put("deletedBy", oldData.getDeletedBy());
                    auditoria.setDatosCompletosAnteriores(objectMapper.writeValueAsString(oldMap));
                } catch (Exception e) {
                    log.error("Error al serializar datos anteriores", e);
                }
            }

            if (newData != null) {
                auditoria.setClienteNombreNuevo(newData.getClienteNombre());
                auditoria.setClienteTelefonoNuevo(newData.getClienteTelefono());
                auditoria.setDireccionEnvioNuevo(newData.getDireccionEnvio());
                auditoria.setTipoTransporteNuevo(newData.getTipoTransporte() != null ? newData.getTipoTransporte().name() : null);
                auditoria.setMaterialNuevo(newData.getMaterial() != null ? newData.getMaterial().name() : null);
                auditoria.setCantidadNuevo(newData.getCantidad());
                auditoria.setMontoTotalNuevo(newData.getMontoTotal());
                auditoria.setAdelantoNuevo(newData.getAdelanto());
                auditoria.setEstadoNuevo(newData.getEstado() != null ? newData.getEstado().name() : null);

                try {
                    Map<String, Object> newMap = new HashMap<>();
                    newMap.put("id", newData.getId());
                    newMap.put("clienteNombre", newData.getClienteNombre());
                    newMap.put("clienteTelefono", newData.getClienteTelefono());
                    newMap.put("direccionEnvio", newData.getDireccionEnvio());
                    newMap.put("tipoTransporte", newData.getTipoTransporte());
                    newMap.put("material", newData.getMaterial());
                    newMap.put("cantidad", newData.getCantidad());
                    newMap.put("montoTotal", newData.getMontoTotal());
                    newMap.put("adelanto", newData.getAdelanto());
                    newMap.put("estado", newData.getEstado());
                    newMap.put("createdAt", newData.getCreatedAt());
                    newMap.put("deletedAt", newData.getDeletedAt());
                    newMap.put("deletedBy", newData.getDeletedBy());
                    auditoria.setDatosCompletosNuevos(objectMapper.writeValueAsString(newMap));
                } catch (Exception e) {
                    log.error("Error al serializar datos nuevos", e);
                }
            }

            auditoriaRepository.save(auditoria);
            log.debug("Auditoría guardada para pedido {}: {}", pedidoId, accion);
        } catch (Exception e) {
            log.error("Error al guardar auditoría para pedido {}: {}", pedidoId, e.getMessage());
        }
    }

    private void auditarActualizacion(Pedido oldData, Pedido newData) {
        try {
            AuditoriaPedido auditoria = new AuditoriaPedido();
            auditoria.setPedidoId(newData.getId());
            auditoria.setAccion("UPDATE");
            auditoria.setUsername(getCurrentUsername());
            auditoria.setFechaHora(LocalDateTime.now());
            auditoria.setIpAddress(getClientIP());

            // Datos anteriores
            auditoria.setClienteNombreAnterior(oldData.getClienteNombre());
            auditoria.setClienteTelefonoAnterior(oldData.getClienteTelefono());
            auditoria.setDireccionEnvioAnterior(oldData.getDireccionEnvio());
            auditoria.setTipoTransporteAnterior(oldData.getTipoTransporte() != null ? oldData.getTipoTransporte().name() : null);
            auditoria.setMaterialAnterior(oldData.getMaterial() != null ? oldData.getMaterial().name() : null);
            auditoria.setCantidadAnterior(oldData.getCantidad());
            auditoria.setMontoTotalAnterior(oldData.getMontoTotal());
            auditoria.setAdelantoAnterior(oldData.getAdelanto());
            auditoria.setEstadoAnterior(oldData.getEstado() != null ? oldData.getEstado().name() : null);

            // Datos nuevos
            auditoria.setClienteNombreNuevo(newData.getClienteNombre());
            auditoria.setClienteTelefonoNuevo(newData.getClienteTelefono());
            auditoria.setDireccionEnvioNuevo(newData.getDireccionEnvio());
            auditoria.setTipoTransporteNuevo(newData.getTipoTransporte() != null ? newData.getTipoTransporte().name() : null);
            auditoria.setMaterialNuevo(newData.getMaterial() != null ? newData.getMaterial().name() : null);
            auditoria.setCantidadNuevo(newData.getCantidad());
            auditoria.setMontoTotalNuevo(newData.getMontoTotal());
            auditoria.setAdelantoNuevo(newData.getAdelanto());
            auditoria.setEstadoNuevo(newData.getEstado() != null ? newData.getEstado().name() : null);

            try {
                Map<String, Object> oldMap = new HashMap<>();
                oldMap.put("id", oldData.getId());
                oldMap.put("clienteNombre", oldData.getClienteNombre());
                oldMap.put("clienteTelefono", oldData.getClienteTelefono());
                oldMap.put("direccionEnvio", oldData.getDireccionEnvio());
                oldMap.put("tipoTransporte", oldData.getTipoTransporte());
                oldMap.put("material", oldData.getMaterial());
                oldMap.put("cantidad", oldData.getCantidad());
                oldMap.put("montoTotal", oldData.getMontoTotal());
                oldMap.put("adelanto", oldData.getAdelanto());
                oldMap.put("estado", oldData.getEstado());
                oldMap.put("createdAt", oldData.getCreatedAt());
                oldMap.put("updatedAt", oldData.getUpdatedAt());
                auditoria.setDatosCompletosAnteriores(objectMapper.writeValueAsString(oldMap));

                Map<String, Object> newMap = new HashMap<>();
                newMap.put("id", newData.getId());
                newMap.put("clienteNombre", newData.getClienteNombre());
                newMap.put("clienteTelefono", newData.getClienteTelefono());
                newMap.put("direccionEnvio", newData.getDireccionEnvio());
                newMap.put("tipoTransporte", newData.getTipoTransporte());
                newMap.put("material", newData.getMaterial());
                newMap.put("cantidad", newData.getCantidad());
                newMap.put("montoTotal", newData.getMontoTotal());
                newMap.put("adelanto", newData.getAdelanto());
                newMap.put("estado", newData.getEstado());
                newMap.put("createdAt", newData.getCreatedAt());
                newMap.put("updatedAt", newData.getUpdatedAt());
                auditoria.setDatosCompletosNuevos(objectMapper.writeValueAsString(newMap));
            } catch (Exception e) {
                log.error("Error al serializar datos para auditoría", e);
            }

            auditoriaRepository.save(auditoria);
            log.debug("Auditoría de actualización guardada para pedido {}", newData.getId());
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

    private PedidoResponseDTO toResponseDTO(Pedido pedido) {
        Transportista transportista = pedido.getTransportista();

        return PedidoResponseDTO.builder()
                .id(pedido.getId())
                .clienteNombre(pedido.getClienteNombre())
                .clienteTelefono(pedido.getClienteTelefono())
                .direccionEnvio(pedido.getDireccionEnvio())
                .tipoTransporte(pedido.getTipoTransporte())
                .material(pedido.getMaterial())
                .cantidad(pedido.getCantidad())
                .montoTotal(pedido.getMontoTotal())
                .adelanto(pedido.getAdelanto())
                .piso(pedido.getPiso())
                .horaEnvio(pedido.getHoraEnvio())
                .transportistaId(transportista != null ? transportista.getId() : null)
                .transportistaNombre(obtenerNombreCompletoTransportista(transportista))
                .estado(pedido.getEstado())
                .codigoVerificacion(pedido.getCodigoVerificacion())
                .build();
    }

    private String obtenerNombreCompletoTransportista(Transportista transportista) {
        if (transportista == null) {
            return null;
        }
        String nombre = transportista.getNombre() != null ? transportista.getNombre().trim() : "";
        String apellidos = transportista.getApellidos() != null ? transportista.getApellidos().trim() : "";
        return (nombre + " " + apellidos).trim();
    }

    private Pedido copiarEntidad(Pedido original) {
        Pedido copia = new Pedido();
        copia.setId(original.getId());
        copia.setCreatedAt(original.getCreatedAt());
        copia.setUpdatedAt(original.getUpdatedAt());
        copia.setDeletedAt(original.getDeletedAt());
        copia.setDeletedBy(original.getDeletedBy());
        copia.setClienteNombre(original.getClienteNombre());
        copia.setClienteTelefono(original.getClienteTelefono());
        copia.setDireccionEnvio(original.getDireccionEnvio());
        copia.setTipoTransporte(original.getTipoTransporte());
        copia.setMaterial(original.getMaterial());
        copia.setCantidad(original.getCantidad());
        copia.setMontoTotal(original.getMontoTotal());
        copia.setAdelanto(original.getAdelanto());
        copia.setPiso(original.getPiso());
        copia.setHoraEnvio(original.getHoraEnvio());
        copia.setEstado(original.getEstado());
        copia.setCodigoVerificacion(original.getCodigoVerificacion());
        copia.setTransportista(original.getTransportista());
        return copia;
    }
}