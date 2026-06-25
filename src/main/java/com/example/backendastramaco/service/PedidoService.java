package com.example.backendastramaco.service;

import com.example.backendastramaco.dto.PedidoRequestDTO;
import com.example.backendastramaco.dto.PedidoResponseDTO;
import com.example.backendastramaco.exception.DuplicateResourceException;
import com.example.backendastramaco.exception.ResourceNotFoundException;
import com.example.backendastramaco.exception.AuditException;
import com.example.backendastramaco.exception.PedidoAlreadyDeletedException;
import com.example.backendastramaco.exception.PedidoNotDeletedException;
import com.example.backendastramaco.exception.CargaNoEncontradaException;
import com.example.backendastramaco.exception.StockInsuficienteException;
import com.example.backendastramaco.exception.TipoTransporteInvalidoException;
import com.example.backendastramaco.exception.MaterialNoPermitidoException;
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
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoService {

    // Constantes para códigos de verificación
    private static final String CODIGO_VERIFICACION_POR_DEFECTO = "1234";

    // Constantes para acciones de auditoría
    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_UPDATE_ESTADO = "UPDATE_ESTADO";
    private static final String ACTION_DELETE = "DELETE";
    private static final String ACTION_RESTORE = "RESTORE";
    private static final String ACTION_DELETE_PERMANENT = "DELETE_PERMANENT";

    // Constantes para claves de mapas
    private static final String KEY_ID = "id";
    private static final String KEY_CLIENTE_NOMBRE = "clienteNombre";
    private static final String KEY_CLIENTE_TELEFONO = "clienteTelefono";
    private static final String KEY_DIRECCION_ENVIO = "direccionEnvio";
    private static final String KEY_TIPO_TRANSPORTE = "tipoTransporte";
    private static final String KEY_MATERIAL = "material";
    private static final String KEY_CANTIDAD = "cantidad";
    private static final String KEY_MONTO_TOTAL = "montoTotal";
    private static final String KEY_ADELANTO = "adelanto";
    private static final String KEY_ESTADO = "estado";
    private static final String KEY_CREATED_AT = "createdAt";
    private static final String KEY_UPDATED_AT = "updatedAt";
    private static final String KEY_DELETED_AT = "deletedAt";
    private static final String KEY_DELETED_BY = "deletedBy";

    // Constantes para mensajes
    private static final String MSG_PEDIDO_NOT_FOUND = "Pedido no encontrado con ID: ";
    private static final String MSG_TRANSPORTISTA_NOT_FOUND = "Transportista no encontrado con ID: ";
    private static final String MSG_PEDIDO_ALREADY_DELETED = "El pedido con ID %d ya está eliminado";
    private static final String MSG_PEDIDO_NOT_DELETED = "El pedido con ID %d no está eliminado";
    private static final String MSG_TIPO_TRANSPORTE_NO_COINCIDE = "El tipo de transporte del pedido no coincide con el transportista seleccionado";
    private static final String MSG_CARGA_NO_ENCONTRADA = "El transportista no tiene carga registrada";
    private static final String MSG_MATERIAL_NO_DISPONIBLE = "El transportista no cuenta con ese material en su carga actual";
    private static final String MSG_STOCK_INSUFICIENTE = "Stock insuficiente para atender el pedido";
    private static final String MSG_MATERIAL_NO_PERMITIDO = "El transportista camionero solo puede trabajar con materiales PANDERETA o TECHO";
    private static final String MSG_AUDIT_SAVE_ERROR = "Error al guardar auditoría para pedido ";
    private static final String MSG_AUDIT_UPDATE_ERROR = "Error al guardar auditoría de actualización para pedido ";
    private static final String MSG_SERIALIZATION_ERROR = "Error al serializar datos del pedido";

    // Constantes para valores
    private static final String DEFAULT_IP = "0.0.0.0";
    private static final String SYSTEM_USER = "sistema";
    private static final String UNKNOWN = "unknown";
    private static final String SPACE = " ";

    // Constantes para headers HTTP
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_PROXY_CLIENT_IP = "Proxy-Client-IP";
    private static final String HEADER_WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";

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
                .orElseThrow(() -> new ResourceNotFoundException(MSG_TRANSPORTISTA_NOT_FOUND + dto.getTransportistaId()));

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
                ACTION_CREATE,
                null,
                pedidoGuardado
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
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PEDIDO_NOT_FOUND + id));
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
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PEDIDO_NOT_FOUND + id));

        Transportista transportista = transportistaRepository.findById(dto.getTransportistaId())
                .orElseThrow(() -> new ResourceNotFoundException(MSG_TRANSPORTISTA_NOT_FOUND + dto.getTransportistaId()));

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
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PEDIDO_NOT_FOUND + id));

        // Guardar copia del estado anterior
        Pedido oldCopy = copiarEntidad(existente);

        // Actualizar estado
        existente.setEstado(EstadoPedido.valueOf(nuevoEstado.toUpperCase()));

        Pedido updated = pedidoRepository.save(existente);
        log.info("Estado del pedido {} cambiado a: {}", id, nuevoEstado);

        // Auditar cambio de estado
        auditarAccion(
                id,
                ACTION_UPDATE_ESTADO,
                oldCopy,
                updated
        );

        return toResponseDTO(updated);
    }

    @Transactional
    public void eliminar(Long id, String username) {
        log.info("Eliminando (soft delete) pedido con ID: {}", id);

        Pedido existente = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PEDIDO_NOT_FOUND + id));

        if (existente.getDeletedAt() != null) {
            throw new PedidoAlreadyDeletedException(
                    String.format(MSG_PEDIDO_ALREADY_DELETED, id)
            );
        }

        // Guardar copia para auditoría
        Pedido oldCopy = copiarEntidad(existente);

        // Soft delete
        existente.softDelete(username);
        pedidoRepository.save(existente);

        // Auditar eliminación
        auditarAccion(
                id,
                ACTION_DELETE,
                oldCopy,
                null
        );

        log.info("Pedido {} eliminado (soft delete) por: {}", id, username);
    }

    @Transactional
    public void restaurar(Long id) {
        log.info("Restaurando pedido con ID: {}", id);

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PEDIDO_NOT_FOUND + id));

        if (pedido.getDeletedAt() == null) {
            throw new PedidoNotDeletedException(
                    String.format(MSG_PEDIDO_NOT_DELETED, id)
            );
        }

        Pedido oldCopy = copiarEntidad(pedido);

        // Restaurar pedido
        pedido.setDeletedAt(null);
        pedido.setDeletedBy(null);
        pedidoRepository.save(pedido);

        // Auditar restauración
        auditarAccion(
                id,
                ACTION_RESTORE,
                oldCopy,
                pedido
        );

        log.info("Pedido {} restaurado", id);
    }

    @Transactional
    public void eliminarPermanente(Long id) {
        log.info("Eliminando permanentemente pedido con ID: {}", id);

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PEDIDO_NOT_FOUND + id));

        // Guardar copia para auditoría antes de eliminar
        Pedido oldCopy = copiarEntidad(pedido);

        // Auditar eliminación permanente
        auditarAccion(
                id,
                ACTION_DELETE_PERMANENT,
                oldCopy,
                null
        );

        pedidoRepository.delete(pedido);
        log.info("Pedido {} eliminado permanentemente", id);
    }

    // ========== MÉTODOS PRIVADOS DE VALIDACIÓN ==========

    private void validarTipoTransporte(PedidoRequestDTO dto, Transportista transportista) {
        if (!transportista.getTipoTransporte().equals(dto.getTipoTransporte())) {
            throw new TipoTransporteInvalidoException(MSG_TIPO_TRANSPORTE_NO_COINCIDE);
        }
    }

    private void procesarCargaSiEsCamionero(PedidoRequestDTO dto, Transportista transportista) {
        if (transportista.getTipoTransporte() != TipoTransporte.CAMIONERO) {
            return;
        }

        validarMaterialCamionero(dto.getMaterial());

        Carga carga = cargaRepository
                .findByTransportistaId(transportista.getId())
                .orElseThrow(() -> new CargaNoEncontradaException(MSG_CARGA_NO_ENCONTRADA));

        if (!carga.getTipoMaterial().equals(dto.getMaterial())) {
            throw new MaterialNoPermitidoException(MSG_MATERIAL_NO_DISPONIBLE);
        }

        if (carga.getCantidadDisponible() < dto.getCantidad()) {
            throw new StockInsuficienteException(MSG_STOCK_INSUFICIENTE);
        }

        carga.setCantidadDisponible(carga.getCantidadDisponible() - dto.getCantidad());
        cargaRepository.save(carga);
    }

    private void validarMaterialCamionero(TipoMaterial material) {
        if (material != TipoMaterial.PANDERETA && material != TipoMaterial.TECHO) {
            throw new MaterialNoPermitidoException(MSG_MATERIAL_NO_PERMITIDO);
        }
    }

    // ========== MÉTODOS DE AUDITORÍA ==========

    private void auditarAccion(Long pedidoId, String accion, Pedido oldData, Pedido newData) {
        try {
            AuditoriaPedido auditoria = crearAuditoriaBase(pedidoId, accion);

            if (oldData != null) {
                mapearDatosAnteriores(auditoria, oldData);
                auditoria.setDatosCompletosAnteriores(serializarPedido(oldData));
            }

            if (newData != null) {
                mapearDatosNuevos(auditoria, newData);
                auditoria.setDatosCompletosNuevos(serializarPedido(newData));
            }

            auditoriaRepository.save(auditoria);
            log.debug("Auditoría guardada para pedido {}: {}", pedidoId, accion);
        } catch (Exception e) {
            log.error(MSG_AUDIT_SAVE_ERROR + "{}: {}", pedidoId, e.getMessage());
            throw new AuditException(MSG_AUDIT_SAVE_ERROR + pedidoId, e);
        }
    }

    private void auditarActualizacion(Pedido oldData, Pedido newData) {
        try {
            AuditoriaPedido auditoria = crearAuditoriaBase(newData.getId(), ACTION_UPDATE);

            mapearDatosAnteriores(auditoria, oldData);
            mapearDatosNuevos(auditoria, newData);

            auditoria.setDatosCompletosAnteriores(serializarPedidoParaActualizacion(oldData));
            auditoria.setDatosCompletosNuevos(serializarPedidoParaActualizacion(newData));

            auditoriaRepository.save(auditoria);
            log.debug("Auditoría de actualización guardada para pedido {}", newData.getId());
        } catch (Exception e) {
            log.error(MSG_AUDIT_UPDATE_ERROR + "{}", newData.getId(), e);
            throw new AuditException(MSG_AUDIT_UPDATE_ERROR + newData.getId(), e);
        }
    }

    private AuditoriaPedido crearAuditoriaBase(Long pedidoId, String accion) {
        AuditoriaPedido auditoria = new AuditoriaPedido();
        auditoria.setPedidoId(pedidoId);
        auditoria.setAccion(accion);
        auditoria.setUsername(getCurrentUsername());
        auditoria.setFechaHora(LocalDateTime.now());
        auditoria.setIpAddress(getClientIP());
        return auditoria;
    }

    private void mapearDatosAnteriores(AuditoriaPedido auditoria, Pedido data) {
        auditoria.setClienteNombreAnterior(data.getClienteNombre());
        auditoria.setClienteTelefonoAnterior(data.getClienteTelefono());
        auditoria.setDireccionEnvioAnterior(data.getDireccionEnvio());
        auditoria.setTipoTransporteAnterior(data.getTipoTransporte() != null ? data.getTipoTransporte().name() : null);
        auditoria.setMaterialAnterior(data.getMaterial() != null ? data.getMaterial().name() : null);
        auditoria.setCantidadAnterior(data.getCantidad());
        auditoria.setMontoTotalAnterior(data.getMontoTotal());
        auditoria.setAdelantoAnterior(data.getAdelanto());
        auditoria.setEstadoAnterior(data.getEstado() != null ? data.getEstado().name() : null);
    }

    private void mapearDatosNuevos(AuditoriaPedido auditoria, Pedido data) {
        auditoria.setClienteNombreNuevo(data.getClienteNombre());
        auditoria.setClienteTelefonoNuevo(data.getClienteTelefono());
        auditoria.setDireccionEnvioNuevo(data.getDireccionEnvio());
        auditoria.setTipoTransporteNuevo(data.getTipoTransporte() != null ? data.getTipoTransporte().name() : null);
        auditoria.setMaterialNuevo(data.getMaterial() != null ? data.getMaterial().name() : null);
        auditoria.setCantidadNuevo(data.getCantidad());
        auditoria.setMontoTotalNuevo(data.getMontoTotal());
        auditoria.setAdelantoNuevo(data.getAdelanto());
        auditoria.setEstadoNuevo(data.getEstado() != null ? data.getEstado().name() : null);
    }

    private String serializarPedido(Pedido pedido) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(KEY_ID, pedido.getId());
            map.put(KEY_CLIENTE_NOMBRE, pedido.getClienteNombre());
            map.put(KEY_CLIENTE_TELEFONO, pedido.getClienteTelefono());
            map.put(KEY_DIRECCION_ENVIO, pedido.getDireccionEnvio());
            map.put(KEY_TIPO_TRANSPORTE, pedido.getTipoTransporte());
            map.put(KEY_MATERIAL, pedido.getMaterial());
            map.put(KEY_CANTIDAD, pedido.getCantidad());
            map.put(KEY_MONTO_TOTAL, pedido.getMontoTotal());
            map.put(KEY_ADELANTO, pedido.getAdelanto());
            map.put(KEY_ESTADO, pedido.getEstado());
            map.put(KEY_CREATED_AT, pedido.getCreatedAt());
            map.put(KEY_DELETED_AT, pedido.getDeletedAt());
            map.put(KEY_DELETED_BY, pedido.getDeletedBy());
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Error al serializar pedido para auditoría", e);
            throw new AuditException(MSG_SERIALIZATION_ERROR, e);
        }
    }

    private String serializarPedidoParaActualizacion(Pedido pedido) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(KEY_ID, pedido.getId());
            map.put(KEY_CLIENTE_NOMBRE, pedido.getClienteNombre());
            map.put(KEY_CLIENTE_TELEFONO, pedido.getClienteTelefono());
            map.put(KEY_DIRECCION_ENVIO, pedido.getDireccionEnvio());
            map.put(KEY_TIPO_TRANSPORTE, pedido.getTipoTransporte());
            map.put(KEY_MATERIAL, pedido.getMaterial());
            map.put(KEY_CANTIDAD, pedido.getCantidad());
            map.put(KEY_MONTO_TOTAL, pedido.getMontoTotal());
            map.put(KEY_ADELANTO, pedido.getAdelanto());
            map.put(KEY_ESTADO, pedido.getEstado());
            map.put(KEY_CREATED_AT, pedido.getCreatedAt());
            map.put(KEY_UPDATED_AT, pedido.getUpdatedAt());
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Error al serializar pedido para auditoría de actualización", e);
            throw new AuditException(MSG_SERIALIZATION_ERROR, e);
        }
    }

    // ========== MÉTODOS AUXILIARES ==========

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            log.debug("No se pudo obtener el usuario autenticado, usando valor por defecto");
            return SYSTEM_USER;
        }
    }

    private String getClientIP() {
        try {
            String ip = request.getHeader(HEADER_X_FORWARDED_FOR);
            if (isInvalidIp(ip)) {
                ip = request.getHeader(HEADER_PROXY_CLIENT_IP);
            }
            if (isInvalidIp(ip)) {
                ip = request.getHeader(HEADER_WL_PROXY_CLIENT_IP);
            }
            if (isInvalidIp(ip)) {
                ip = request.getRemoteAddr();
            }
            return isInvalidIp(ip) ? DEFAULT_IP : ip;
        } catch (Exception e) {
            log.debug("No se pudo obtener la IP del cliente, usando valor por defecto");
            return DEFAULT_IP;
        }
    }

    private boolean isInvalidIp(String ip) {
        return ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip);
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
        return (nombre + SPACE + apellidos).trim();
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