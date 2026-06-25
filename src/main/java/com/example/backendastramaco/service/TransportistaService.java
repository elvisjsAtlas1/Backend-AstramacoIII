package com.example.backendastramaco.service;

import com.example.backendastramaco.dto.TransportistaRequestDTO;
import com.example.backendastramaco.exception.DuplicateResourceException;
import com.example.backendastramaco.exception.ResourceNotFoundException;
import com.example.backendastramaco.exception.AuditException;
import com.example.backendastramaco.exception.TransportistaAlreadyDeletedException;
import com.example.backendastramaco.exception.TransportistaNotDeletedException;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.audit.AuditoriaTransportista;
import com.example.backendastramaco.model.enums.EstadoTransportista;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.repository.audit.AuditoriaTransportistaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransportistaService {

    // Constantes para acciones de auditoría
    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_UPDATE_ESTADO = "UPDATE_ESTADO";
    private static final String ACTION_DELETE = "DELETE";
    private static final String ACTION_RESTORE = "RESTORE";
    private static final String ACTION_DELETE_PERMANENT = "DELETE_PERMANENT";

    // Constantes para claves de mapas
    private static final String KEY_ID = "id";
    private static final String KEY_NOMBRE = "nombre";
    private static final String KEY_APELLIDOS = "apellidos";
    private static final String KEY_DNI = "dni";
    private static final String KEY_EDAD = "edad";
    private static final String KEY_TIPO_TRANSPORTE = "tipoTransporte";
    private static final String KEY_PLACA = "placa";
    private static final String KEY_VEHICULO_INFO = "vehiculoInfo";
    private static final String KEY_CAPACIDAD = "capacidad";
    private static final String KEY_ESTADO = "estado";
    private static final String KEY_CREATED_AT = "createdAt";
    private static final String KEY_UPDATED_AT = "updatedAt";
    private static final String KEY_DELETED_AT = "deletedAt";
    private static final String KEY_DELETED_BY = "deletedBy";

    // Constantes para mensajes
    private static final String MSG_TRANSPORTISTA_NOT_FOUND = "Transportista no encontrado con ID: ";
    private static final String MSG_TRANSPORTISTA_NOT_FOUND_BY_DNI = "Transportista no encontrado con DNI: ";
    private static final String MSG_TRANSPORTISTA_NOT_FOUND_BY_USER = "Transportista no encontrado para el usuario: ";
    private static final String MSG_USUARIO_NOT_FOUND = "Usuario no encontrado con ID: ";
    private static final String MSG_DUPLICATE_DNI = "El DNI '%s' ya está registrado";
    private static final String MSG_DUPLICATE_PLACA = "La placa '%s' ya está registrada";
    private static final String MSG_TRANSPORTISTA_ALREADY_DELETED = "El transportista con ID %d ya está eliminado";
    private static final String MSG_TRANSPORTISTA_NOT_DELETED = "El transportista con ID %d no está eliminado";
    private static final String MSG_AUDIT_SAVE_ERROR = "Error al guardar auditoría para transportista ";
    private static final String MSG_AUDIT_UPDATE_ERROR = "Error al guardar auditoría de actualización para transportista ";
    private static final String MSG_SERIALIZATION_ERROR = "Error al serializar datos del transportista";

    // Constantes para valores
    private static final String DEFAULT_IP = "0.0.0.0";
    private static final String SYSTEM_USER = "sistema";
    private static final String UNKNOWN = "unknown";
    private static final String USERNAME_SEPARATOR = ".";
    private static final double CAPACIDAD_CAMIONERO = 0.0;

    // Constantes para headers HTTP
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_PROXY_CLIENT_IP = "Proxy-Client-IP";
    private static final String HEADER_WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";

    private final TransportistaRepository transportistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaTransportistaRepository auditoriaRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Transactional
    public Transportista crear(TransportistaRequestDTO dto) {
        log.info("Creando nuevo transportista: {}", dto.getNombre());

        // Validar DNI único
        if (transportistaRepository.existsByDni(dto.getDni())) {
            throw new DuplicateResourceException(String.format(MSG_DUPLICATE_DNI, dto.getDni()));
        }

        // Validar placa única
        if (transportistaRepository.existsByPlaca(dto.getPlaca())) {
            throw new DuplicateResourceException(String.format(MSG_DUPLICATE_PLACA, dto.getPlaca()));
        }

        // CREAR USUARIO AUTOMÁTICAMENTE
        Usuario usuario = new Usuario();
        String username = generarUsernameUnico(dto.getNombre(), dto.getApellidos());
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(dto.getDni())); // Contraseña = DNI
        usuario.setRol(Rol.TRANSPORTISTA);
        usuario.setActivo(true);
        usuario = usuarioRepository.save(usuario);

        Transportista t = new Transportista();
        t.setUsuario(usuario);
        t.setNombre(dto.getNombre());
        t.setApellidos(dto.getApellidos());
        t.setDni(dto.getDni());
        t.setEdad(dto.getEdad());
        t.setTipoTransporte(dto.getTipoTransporte());
        t.setPlaca(dto.getPlaca());
        t.setVehiculoInfo(dto.getVehiculoInfo());

        // Si es CAMIONERO, capacidad es 0
        if (dto.getTipoTransporte() == TipoTransporte.CAMIONERO) {
            t.setCapacidad(CAPACIDAD_CAMIONERO);
        } else {
            t.setCapacidad(dto.getCapacidad());
        }

        t.setEstado(EstadoTransportista.ACTIVO);

        Transportista saved = transportistaRepository.save(t);
        log.info("Transportista creado con ID: {}, Usuario creado: {}", saved.getId(), username);

        // Auditar creación
        auditarAccion(
                saved.getId(),
                ACTION_CREATE,
                null,
                saved
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Transportista> listar(Pageable pageable, String tipoTransporte, String estado) {
        log.debug("Listando transportistas con filtros - tipo: {}, estado: {}", tipoTransporte, estado);

        if (tipoTransporte != null && estado != null) {
            return transportistaRepository.findByTipoTransporteAndEstadoAndDeletedAtIsNull(
                    TipoTransporte.valueOf(tipoTransporte.toUpperCase()),
                    EstadoTransportista.valueOf(estado.toUpperCase()),
                    pageable);
        } else if (tipoTransporte != null) {
            return transportistaRepository.findByTipoTransporteAndDeletedAtIsNull(
                    TipoTransporte.valueOf(tipoTransporte.toUpperCase()),
                    pageable);
        } else if (estado != null) {
            return transportistaRepository.findByEstadoAndDeletedAtIsNull(
                    EstadoTransportista.valueOf(estado.toUpperCase()),
                    pageable);
        } else {
            return transportistaRepository.findByDeletedAtIsNull(pageable);
        }
    }

    @Transactional(readOnly = true)
    public Page<Transportista> listarTodos(Pageable pageable) {
        log.debug("Listando todos los transportistas (incluyendo eliminados)");
        return transportistaRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transportista> listarEliminados(Pageable pageable) {
        log.debug("Listando transportistas eliminados");
        return transportistaRepository.findByDeletedAtIsNotNull(pageable);
    }

    @Transactional(readOnly = true)
    public Transportista obtenerPorId(Long id) {
        log.debug("Obteniendo transportista por ID: {}", id);
        return transportistaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_TRANSPORTISTA_NOT_FOUND + id));
    }

    @Transactional(readOnly = true)
    public Transportista obtenerPorDni(String dni) {
        log.debug("Obteniendo transportista por DNI: {}", dni);
        return transportistaRepository.findByDniAndDeletedAtIsNull(dni)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_TRANSPORTISTA_NOT_FOUND_BY_DNI + dni));
    }

    @Transactional(readOnly = true)
    public Transportista obtenerPorUsuarioId(Long usuarioId) {
        log.debug("Obteniendo transportista por usuario ID: {}", usuarioId);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_USUARIO_NOT_FOUND + usuarioId));
        return transportistaRepository.findByUsuarioAndDeletedAtIsNull(usuario)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_TRANSPORTISTA_NOT_FOUND_BY_USER + usuarioId));
    }

    @Transactional(readOnly = true)
    public List<Transportista> listarPorTipo(TipoTransporte tipo) {
        log.debug("Listando transportistas por tipo: {}", tipo);
        return transportistaRepository.findByTipoTransporteAndEstadoAndDeletedAtIsNull(tipo, EstadoTransportista.ACTIVO);
    }

    @Transactional
    public Transportista actualizar(Long id, TransportistaRequestDTO dto) {
        log.info("Actualizando transportista con ID: {}", id);

        Transportista existente = transportistaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_TRANSPORTISTA_NOT_FOUND + id));

        // Validar DNI único (excepto el mismo)
        if (!existente.getDni().equals(dto.getDni()) &&
                transportistaRepository.existsByDni(dto.getDni())) {
            throw new DuplicateResourceException(String.format(MSG_DUPLICATE_DNI, dto.getDni()));
        }

        // Validar placa única (excepto el mismo)
        if (!existente.getPlaca().equals(dto.getPlaca()) &&
                transportistaRepository.existsByPlaca(dto.getPlaca())) {
            throw new DuplicateResourceException(String.format(MSG_DUPLICATE_PLACA, dto.getPlaca()));
        }

        // Guardar copia del estado anterior para auditoría
        Transportista oldCopy = copiarEntidad(existente);

        // Actualizar campos
        existente.setNombre(dto.getNombre());
        existente.setApellidos(dto.getApellidos());
        existente.setDni(dto.getDni());
        existente.setEdad(dto.getEdad());
        existente.setTipoTransporte(dto.getTipoTransporte());
        existente.setPlaca(dto.getPlaca());
        existente.setVehiculoInfo(dto.getVehiculoInfo());

        // Si es CAMIONERO, capacidad es 0 (no aplica)
        if (dto.getTipoTransporte() == TipoTransporte.CAMIONERO) {
            existente.setCapacidad(CAPACIDAD_CAMIONERO);
        } else {
            existente.setCapacidad(dto.getCapacidad());
        }

        Transportista updated = transportistaRepository.save(existente);
        log.info("Transportista actualizado con ID: {}", id);

        // Auditar actualización
        auditarActualizacion(oldCopy, updated);

        return updated;
    }

    @Transactional
    public void cambiarEstado(Long id, String estado) {
        log.info("Cambiando estado del transportista con ID: {} a {}", id, estado);

        Transportista transportista = transportistaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_TRANSPORTISTA_NOT_FOUND + id));

        Transportista oldCopy = copiarEntidad(transportista);
        transportista.setEstado(EstadoTransportista.valueOf(estado.toUpperCase()));
        transportistaRepository.save(transportista);

        // Auditar cambio de estado
        auditarAccion(
                id,
                ACTION_UPDATE_ESTADO,
                oldCopy,
                transportista
        );

        log.info("Estado del transportista {} cambiado a: {}", id, estado);
    }

    @Transactional
    public void eliminar(Long id, String username) {
        log.info("Eliminando (soft delete) transportista con ID: {}", id);

        Transportista existente = transportistaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_TRANSPORTISTA_NOT_FOUND + id));

        if (existente.getDeletedAt() != null) {
            throw new TransportistaAlreadyDeletedException(
                    String.format(MSG_TRANSPORTISTA_ALREADY_DELETED, id)
            );
        }

        // Guardar copia para auditoría
        Transportista oldCopy = copiarEntidad(existente);

        // Soft delete
        existente.softDelete(username);
        existente.setEstado(EstadoTransportista.INACTIVO);

        transportistaRepository.save(existente);

        // Auditar eliminación
        auditarAccion(
                id,
                ACTION_DELETE,
                oldCopy,
                null
        );

        log.info("Transportista {} eliminado (soft delete) por: {}", id, username);
    }

    @Transactional
    public void restaurar(Long id) {
        log.info("Restaurando transportista con ID: {}", id);

        Transportista transportista = transportistaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_TRANSPORTISTA_NOT_FOUND + id));

        if (transportista.getDeletedAt() == null) {
            throw new TransportistaNotDeletedException(
                    String.format(MSG_TRANSPORTISTA_NOT_DELETED, id)
            );
        }

        Transportista oldCopy = copiarEntidad(transportista);

        // Restaurar transportista
        transportista.setDeletedAt(null);
        transportista.setDeletedBy(null);
        transportista.setEstado(EstadoTransportista.ACTIVO);

        transportistaRepository.save(transportista);

        // Auditar restauración
        auditarAccion(
                id,
                ACTION_RESTORE,
                oldCopy,
                transportista
        );

        log.info("Transportista {} restaurado", id);
    }

    @Transactional
    public void eliminarPermanente(Long id) {
        log.info("Eliminando permanentemente transportista con ID: {}", id);

        Transportista transportista = transportistaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_TRANSPORTISTA_NOT_FOUND + id));

        // Guardar copia para auditoría antes de eliminar
        Transportista oldCopy = copiarEntidad(transportista);

        // Auditar eliminación permanente
        auditarAccion(
                id,
                ACTION_DELETE_PERMANENT,
                oldCopy,
                null
        );

        transportistaRepository.delete(transportista);
        log.info("Transportista {} eliminado permanentemente", id);
    }

    // Métodos de auditoría privados

    private void auditarAccion(Long transportistaId, String accion, Transportista oldData, Transportista newData) {
        try {
            AuditoriaTransportista auditoria = crearAuditoriaBase(transportistaId, accion);

            if (oldData != null) {
                mapearDatosAnteriores(auditoria, oldData);
                auditoria.setDatosCompletosAnteriores(serializarTransportista(oldData));
            }

            if (newData != null) {
                mapearDatosNuevos(auditoria, newData);
                auditoria.setDatosCompletosNuevos(serializarTransportista(newData));
            }

            auditoriaRepository.save(auditoria);
            log.debug("Auditoría guardada para transportista {}: {}", transportistaId, accion);
        } catch (Exception e) {
            log.error(MSG_AUDIT_SAVE_ERROR + "{}: {}", transportistaId, e.getMessage());
            throw new AuditException(MSG_AUDIT_SAVE_ERROR + transportistaId, e);
        }
    }

    private void auditarActualizacion(Transportista oldData, Transportista newData) {
        try {
            AuditoriaTransportista auditoria = crearAuditoriaBase(newData.getId(), ACTION_UPDATE);

            mapearDatosAnteriores(auditoria, oldData);
            mapearDatosNuevos(auditoria, newData);

            auditoria.setDatosCompletosAnteriores(serializarTransportistaParaActualizacion(oldData));
            auditoria.setDatosCompletosNuevos(serializarTransportistaParaActualizacion(newData));

            auditoriaRepository.save(auditoria);
            log.debug("Auditoría de actualización guardada para transportista {}", newData.getId());
        } catch (Exception e) {
            log.error(MSG_AUDIT_UPDATE_ERROR + "{}", newData.getId(), e);
            throw new AuditException(MSG_AUDIT_UPDATE_ERROR + newData.getId(), e);
        }
    }

    private AuditoriaTransportista crearAuditoriaBase(Long transportistaId, String accion) {
        AuditoriaTransportista auditoria = new AuditoriaTransportista();
        auditoria.setTransportistaId(transportistaId);
        auditoria.setAccion(accion);
        auditoria.setUsername(getCurrentUsername());
        auditoria.setFechaHora(LocalDateTime.now());
        auditoria.setIpAddress(getClientIP());
        return auditoria;
    }

    private void mapearDatosAnteriores(AuditoriaTransportista auditoria, Transportista data) {
        auditoria.setNombreAnterior(data.getNombre());
        auditoria.setApellidosAnterior(data.getApellidos());
        auditoria.setDniAnterior(data.getDni());
        auditoria.setEdadAnterior(data.getEdad());
        auditoria.setTipoTransporteAnterior(data.getTipoTransporte() != null ? data.getTipoTransporte().name() : null);
        auditoria.setPlacaAnterior(data.getPlaca());
        auditoria.setVehiculoInfoAnterior(data.getVehiculoInfo());
        auditoria.setCapacidadAnterior(data.getCapacidad());
        auditoria.setEstadoAnterior(data.getEstado() != null ? data.getEstado().name() : null);
    }

    private void mapearDatosNuevos(AuditoriaTransportista auditoria, Transportista data) {
        auditoria.setNombreNuevo(data.getNombre());
        auditoria.setApellidosNuevo(data.getApellidos());
        auditoria.setDniNuevo(data.getDni());
        auditoria.setEdadNuevo(data.getEdad());
        auditoria.setTipoTransporteNuevo(data.getTipoTransporte() != null ? data.getTipoTransporte().name() : null);
        auditoria.setPlacaNuevo(data.getPlaca());
        auditoria.setVehiculoInfoNuevo(data.getVehiculoInfo());
        auditoria.setCapacidadNuevo(data.getCapacidad());
        auditoria.setEstadoNuevo(data.getEstado() != null ? data.getEstado().name() : null);
    }

    private String serializarTransportista(Transportista transportista) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(KEY_ID, transportista.getId());
            map.put(KEY_NOMBRE, transportista.getNombre());
            map.put(KEY_APELLIDOS, transportista.getApellidos());
            map.put(KEY_DNI, transportista.getDni());
            map.put(KEY_EDAD, transportista.getEdad());
            map.put(KEY_TIPO_TRANSPORTE, transportista.getTipoTransporte());
            map.put(KEY_PLACA, transportista.getPlaca());
            map.put(KEY_VEHICULO_INFO, transportista.getVehiculoInfo());
            map.put(KEY_CAPACIDAD, transportista.getCapacidad());
            map.put(KEY_ESTADO, transportista.getEstado());
            map.put(KEY_CREATED_AT, transportista.getCreatedAt());
            map.put(KEY_DELETED_AT, transportista.getDeletedAt());
            map.put(KEY_DELETED_BY, transportista.getDeletedBy());
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Error al serializar transportista para auditoría", e);
            throw new AuditException(MSG_SERIALIZATION_ERROR, e);
        }
    }

    private String serializarTransportistaParaActualizacion(Transportista transportista) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(KEY_ID, transportista.getId());
            map.put(KEY_NOMBRE, transportista.getNombre());
            map.put(KEY_APELLIDOS, transportista.getApellidos());
            map.put(KEY_DNI, transportista.getDni());
            map.put(KEY_EDAD, transportista.getEdad());
            map.put(KEY_TIPO_TRANSPORTE, transportista.getTipoTransporte());
            map.put(KEY_PLACA, transportista.getPlaca());
            map.put(KEY_VEHICULO_INFO, transportista.getVehiculoInfo());
            map.put(KEY_CAPACIDAD, transportista.getCapacidad());
            map.put(KEY_ESTADO, transportista.getEstado());
            map.put(KEY_CREATED_AT, transportista.getCreatedAt());
            map.put(KEY_UPDATED_AT, transportista.getUpdatedAt());
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Error al serializar transportista para auditoría de actualización", e);
            throw new AuditException(MSG_SERIALIZATION_ERROR, e);
        }
    }

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

    private Transportista copiarEntidad(Transportista original) {
        Transportista copia = new Transportista();
        copia.setId(original.getId());
        copia.setCreatedAt(original.getCreatedAt());
        copia.setUpdatedAt(original.getUpdatedAt());
        copia.setDeletedAt(original.getDeletedAt());
        copia.setDeletedBy(original.getDeletedBy());
        copia.setNombre(original.getNombre());
        copia.setApellidos(original.getApellidos());
        copia.setDni(original.getDni());
        copia.setEdad(original.getEdad());
        copia.setTipoTransporte(original.getTipoTransporte());
        copia.setPlaca(original.getPlaca());
        copia.setVehiculoInfo(original.getVehiculoInfo());
        copia.setCapacidad(original.getCapacidad());
        copia.setEstado(original.getEstado());
        copia.setUsuario(original.getUsuario());
        return copia;
    }

    private String generarUsernameUnico(String nombre, String apellidos) {
        String base = generarUsername(nombre, apellidos);
        String username = base;
        int contador = 1;

        while (usuarioRepository.findByUsername(username).isPresent()) {
            username = base + contador;
            contador++;
        }

        return username;
    }

    private String generarUsername(String nombre, String apellidos) {
        String primerNombre = nombre.split(" ")[0].toLowerCase(Locale.ROOT);
        String primerApellido = apellidos.split(" ")[0].toLowerCase(Locale.ROOT);
        return primerNombre + USERNAME_SEPARATOR + primerApellido;
    }
}