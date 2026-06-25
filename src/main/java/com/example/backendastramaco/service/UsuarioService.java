package com.example.backendastramaco.service;

import com.example.backendastramaco.dto.UsuarioRequestDTO;
import com.example.backendastramaco.exception.ResourceNotFoundException;
import com.example.backendastramaco.exception.DuplicateResourceException;
import com.example.backendastramaco.exception.AuditException;
import com.example.backendastramaco.exception.UserAlreadyDeletedException;
import com.example.backendastramaco.exception.UserNotDeletedException;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.audit.AuditoriaUsuario;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.repository.audit.AuditoriaUsuarioRepository;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    // Constantes para acciones de auditoría
    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_UPDATE_ESTADO = "UPDATE_ESTADO";
    private static final String ACTION_DELETE = "DELETE";
    private static final String ACTION_RESTORE = "RESTORE";
    private static final String ACTION_DELETE_PERMANENT = "DELETE_PERMANENT";

    // Constantes para claves de mapas
    private static final String KEY_ID = "id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROL = "rol";
    private static final String KEY_ACTIVO = "activo";
    private static final String KEY_CREATED_AT = "createdAt";
    private static final String KEY_UPDATED_AT = "updatedAt";
    private static final String KEY_DELETED_AT = "deletedAt";
    private static final String KEY_DELETED_BY = "deletedBy";

    // Constantes para mensajes
    private static final String MSG_USER_NOT_FOUND = "Usuario no encontrado con ID: ";
    private static final String MSG_USER_NOT_FOUND_BY_USERNAME = "Usuario no encontrado con username: ";
    private static final String MSG_DUPLICATE_USERNAME = "El username '%s' ya está registrado";
    private static final String MSG_USER_ALREADY_DELETED = "El usuario con ID %d ya está eliminado";
    private static final String MSG_USER_NOT_DELETED = "El usuario con ID %d no está eliminado";
    private static final String MSG_AUDIT_SAVE_ERROR = "Error al guardar auditoría para usuario ";
    private static final String MSG_AUDIT_UPDATE_ERROR = "Error al guardar auditoría de actualización para usuario ";
    private static final String MSG_SERIALIZATION_ERROR = "Error al serializar datos del usuario";

    // Constantes para valores de auditoría
    private static final String PASSWORD_CHANGED_YES = "SI";
    private static final String PASSWORD_CHANGED_NO = "NO";
    private static final String DEFAULT_IP = "0.0.0.0";
    private static final String SYSTEM_USER = "sistema";
    private static final String UNKNOWN = "unknown";

    // Constantes para headers HTTP
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_PROXY_CLIENT_IP = "Proxy-Client-IP";
    private static final String HEADER_WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";

    // Constante para el marcador de posición de password en auditoría
    private static final String AUDIT_PASSWORD_PLACEHOLDER = "[PROTEGIDO]";

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaUsuarioRepository auditoriaRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Transactional
    public Usuario crear(UsuarioRequestDTO dto) {
        log.info("Creando nuevo usuario: {}", dto.getUsername());

        // Verificar si el username ya existe
        if (usuarioRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException(String.format(MSG_DUPLICATE_USERNAME, dto.getUsername()));
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(dto.getUsername());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setRol(dto.getRol());
        usuario.setActivo(true);

        Usuario saved = usuarioRepository.save(usuario);
        log.info("Usuario creado con ID: {}", saved.getId());

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
    public Page<Usuario> listar(Pageable pageable, String rol, Boolean activo) {
        log.debug("Listando usuarios activos con filtros - rol: {}, activo: {}", rol, activo);

        if (rol != null && activo != null) {
            return usuarioRepository.findByRolAndActivoAndDeletedAtIsNull(rol, activo, pageable);
        } else if (rol != null) {
            return usuarioRepository.findByRolAndDeletedAtIsNull(rol, pageable);
        } else if (activo != null) {
            return usuarioRepository.findByActivoAndDeletedAtIsNull(activo, pageable);
        } else {
            return usuarioRepository.findByDeletedAtIsNull(pageable);
        }
    }

    @Transactional(readOnly = true)
    public Page<Usuario> listarTodos(Pageable pageable) {
        log.debug("Listando todos los usuarios (incluyendo eliminados)");
        return usuarioRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Usuario> listarEliminados(Pageable pageable) {
        log.debug("Listando usuarios eliminados");
        return usuarioRepository.findByDeletedAtIsNotNull(pageable);
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorId(Long id) {
        log.debug("Obteniendo usuario por ID: {}", id);
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_USER_NOT_FOUND + id));
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorUsername(String username) {
        log.debug("Obteniendo usuario por username: {}", username);
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_USER_NOT_FOUND_BY_USERNAME + username));
    }

    @Transactional
    public Usuario actualizar(Long id, UsuarioRequestDTO dto) {
        log.info("Actualizando usuario con ID: {}", id);

        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_USER_NOT_FOUND + id));

        // Verificar si el nuevo username ya existe (excepto el mismo usuario)
        if (!existente.getUsername().equals(dto.getUsername()) &&
                usuarioRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException(String.format(MSG_DUPLICATE_USERNAME, dto.getUsername()));
        }

        // Guardar copia del estado anterior para auditoría
        Usuario oldCopy = copiarEntidad(existente);

        // Actualizar campos
        boolean passwordCambiada = false;
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            existente.setPassword(passwordEncoder.encode(dto.getPassword()));
            passwordCambiada = true;
        }
        existente.setUsername(dto.getUsername());
        existente.setRol(dto.getRol());

        Usuario updated = usuarioRepository.save(existente);
        log.info("Usuario actualizado con ID: {}", id);

        // Auditar actualización
        auditarActualizacion(oldCopy, updated, passwordCambiada);

        return updated;
    }

    @Transactional
    public void cambiarEstado(Long id, Boolean activo) {
        log.info("Cambiando estado del usuario con ID: {} a activo: {}", id, activo);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_USER_NOT_FOUND + id));

        Usuario oldCopy = copiarEntidad(usuario);
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);

        // Auditar cambio de estado
        auditarAccion(
                id,
                ACTION_UPDATE_ESTADO,
                oldCopy,
                usuario
        );

        log.info("Estado del usuario {} cambiado a: {}", id, activo);
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando (soft delete) usuario con ID: {}", id);

        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_USER_NOT_FOUND + id));

        if (existente.getDeletedAt() != null) {
            throw new UserAlreadyDeletedException(String.format(MSG_USER_ALREADY_DELETED, id));
        }

        // Guardar copia para auditoría
        Usuario oldCopy = copiarEntidad(existente);

        // Obtener usuario actual para auditoría
        String currentUser = getCurrentUsername();

        // Soft delete
        existente.softDelete(currentUser);
        usuarioRepository.save(existente);

        // Auditar eliminación
        auditarAccion(
                id,
                ACTION_DELETE,
                oldCopy,
                null
        );

        log.info("Usuario {} eliminado (soft delete) por: {}", id, currentUser);
    }

    @Transactional
    public void restaurar(Long id) {
        log.info("Restaurando usuario con ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_USER_NOT_FOUND + id));

        if (usuario.getDeletedAt() == null) {
            throw new UserNotDeletedException(String.format(MSG_USER_NOT_DELETED, id));
        }

        Usuario oldCopy = copiarEntidad(usuario);

        // Restaurar usuario
        usuario.setDeletedAt(null);
        usuario.setDeletedBy(null);
        usuarioRepository.save(usuario);

        // Auditar restauración
        auditarAccion(
                id,
                ACTION_RESTORE,
                oldCopy,
                usuario
        );

        log.info("Usuario {} restaurado", id);
    }

    @Transactional
    public void eliminarPermanente(Long id) {
        log.info("Eliminando permanentemente usuario con ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_USER_NOT_FOUND + id));

        // Guardar copia para auditoría antes de eliminar
        Usuario oldCopy = copiarEntidad(usuario);

        // Auditar eliminación permanente
        auditarAccion(
                id,
                ACTION_DELETE_PERMANENT,
                oldCopy,
                null
        );

        usuarioRepository.delete(usuario);
        log.info("Usuario {} eliminado permanentemente", id);
    }

    // Métodos de auditoría privados

    private void auditarAccion(Long usuarioId, String accion, Usuario oldData, Usuario newData) {
        try {
            AuditoriaUsuario auditoria = crearAuditoriaBase(usuarioId, accion);

            if (oldData != null) {
                auditoria.setUsernameAnterior(oldData.getUsername());
                auditoria.setRolAnterior(oldData.getRol().name());
                auditoria.setPasswordAnterior(AUDIT_PASSWORD_PLACEHOLDER);
                auditoria.setDatosCompletosAnteriores(serializarUsuario(oldData));
            }

            if (newData != null) {
                auditoria.setUsernameNuevo(newData.getUsername());
                auditoria.setRolNuevo(newData.getRol().name());
                auditoria.setPasswordCambiada(PASSWORD_CHANGED_NO);
                auditoria.setDatosCompletosNuevos(serializarUsuario(newData));
            }

            auditoriaRepository.save(auditoria);
            log.debug("Auditoría guardada para usuario {}: {}", usuarioId, accion);
        } catch (Exception e) {
            log.error(MSG_AUDIT_SAVE_ERROR + "{}: {}", usuarioId, e.getMessage());
            throw new AuditException(MSG_AUDIT_SAVE_ERROR + usuarioId, e);
        }
    }

    private void auditarActualizacion(Usuario oldData, Usuario newData, boolean passwordCambiada) {
        try {
            AuditoriaUsuario auditoria = crearAuditoriaBase(newData.getId(), ACTION_UPDATE);

            auditoria.setUsernameAnterior(oldData.getUsername());
            auditoria.setRolAnterior(oldData.getRol().name());
            auditoria.setPasswordAnterior(AUDIT_PASSWORD_PLACEHOLDER);

            auditoria.setUsernameNuevo(newData.getUsername());
            auditoria.setRolNuevo(newData.getRol().name());
            auditoria.setPasswordCambiada(passwordCambiada ? PASSWORD_CHANGED_YES : PASSWORD_CHANGED_NO);

            auditoria.setDatosCompletosAnteriores(serializarUsuarioParaActualizacion(oldData));
            auditoria.setDatosCompletosNuevos(serializarUsuarioParaActualizacion(newData));

            auditoriaRepository.save(auditoria);
            log.debug("Auditoría de actualización guardada para usuario {}", newData.getId());
        } catch (Exception e) {
            log.error(MSG_AUDIT_UPDATE_ERROR + "{}", newData.getId(), e);
            throw new AuditException(MSG_AUDIT_UPDATE_ERROR + newData.getId(), e);
        }
    }

    private AuditoriaUsuario crearAuditoriaBase(Long usuarioId, String accion) {
        AuditoriaUsuario auditoria = new AuditoriaUsuario();
        auditoria.setUsuarioId(usuarioId);
        auditoria.setAccion(accion);
        auditoria.setUsername(getCurrentUsername());
        auditoria.setFechaHora(LocalDateTime.now());
        auditoria.setIpAddress(getClientIP());
        return auditoria;
    }

    private String serializarUsuario(Usuario usuario) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(KEY_ID, usuario.getId());
            map.put(KEY_USERNAME, usuario.getUsername());
            map.put(KEY_ROL, usuario.getRol());
            map.put(KEY_ACTIVO, usuario.getActivo());
            map.put(KEY_CREATED_AT, usuario.getCreatedAt());
            map.put(KEY_DELETED_AT, usuario.getDeletedAt());
            map.put(KEY_DELETED_BY, usuario.getDeletedBy());
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Error al serializar usuario para auditoría", e);
            throw new AuditException(MSG_SERIALIZATION_ERROR, e);
        }
    }

    private String serializarUsuarioParaActualizacion(Usuario usuario) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(KEY_ID, usuario.getId());
            map.put(KEY_USERNAME, usuario.getUsername());
            map.put(KEY_ROL, usuario.getRol());
            map.put(KEY_ACTIVO, usuario.getActivo());
            map.put(KEY_CREATED_AT, usuario.getCreatedAt());
            map.put(KEY_UPDATED_AT, usuario.getUpdatedAt());
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Error al serializar usuario para auditoría de actualización", e);
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

    private Usuario copiarEntidad(Usuario original) {
        Usuario copia = new Usuario();
        copia.setId(original.getId());
        copia.setCreatedAt(original.getCreatedAt());
        copia.setUpdatedAt(original.getUpdatedAt());
        copia.setDeletedAt(original.getDeletedAt());
        copia.setDeletedBy(original.getDeletedBy());
        copia.setUsername(original.getUsername());
        copia.setPassword(original.getPassword());
        copia.setRol(original.getRol());
        copia.setActivo(original.getActivo());
        return copia;
    }
}