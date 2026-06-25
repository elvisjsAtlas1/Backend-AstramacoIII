package com.example.backendastramaco.service;

import com.example.backendastramaco.dto.UsuarioRequestDTO;
import com.example.backendastramaco.exception.ResourceNotFoundException;
import com.example.backendastramaco.exception.DuplicateResourceException;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.audit.AuditoriaUsuario;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.repository.audit.AuditoriaUsuarioRepository;
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
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

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
            throw new DuplicateResourceException("El username '" + dto.getUsername() + "' ya está registrado");
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
                "CREATE",
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
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorUsername(String username) {
        log.debug("Obteniendo usuario por username: {}", username);
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con username: " + username));
    }

    @Transactional
    public Usuario actualizar(Long id, UsuarioRequestDTO dto) {
        log.info("Actualizando usuario con ID: {}", id);

        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        // Verificar si el nuevo username ya existe (excepto el mismo usuario)
        if (!existente.getUsername().equals(dto.getUsername()) &&
                usuarioRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("El username '" + dto.getUsername() + "' ya está registrado");
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
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        Usuario oldCopy = copiarEntidad(usuario);
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);

        // Auditar cambio de estado
        auditarAccion(
                id,
                "UPDATE_ESTADO",
                oldCopy,
                usuario
        );

        log.info("Estado del usuario {} cambiado a: {}", id, activo);
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando (soft delete) usuario con ID: {}", id);

        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        if (existente.getDeletedAt() != null) {
            throw new RuntimeException("El usuario ya está eliminado");
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
                "DELETE",
                oldCopy,
                null
        );

        log.info("Usuario {} eliminado (soft delete) por: {}", id, currentUser);
    }

    @Transactional
    public void restaurar(Long id) {
        log.info("Restaurando usuario con ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        if (usuario.getDeletedAt() == null) {
            throw new RuntimeException("El usuario no está eliminado");
        }

        Usuario oldCopy = copiarEntidad(usuario);

        // Restaurar usuario
        usuario.setDeletedAt(null);
        usuario.setDeletedBy(null);
        usuarioRepository.save(usuario);

        // Auditar restauración
        auditarAccion(
                id,
                "RESTORE",
                oldCopy,
                usuario
        );

        log.info("Usuario {} restaurado", id);
    }

    @Transactional
    public void eliminarPermanente(Long id) {
        log.info("Eliminando permanentemente usuario con ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        // Guardar copia para auditoría antes de eliminar
        Usuario oldCopy = copiarEntidad(usuario);

        // Auditar eliminación permanente
        auditarAccion(
                id,
                "DELETE_PERMANENT",
                oldCopy,
                null
        );

        usuarioRepository.delete(usuario);
        log.info("Usuario {} eliminado permanentemente", id);
    }

    // Métodos de auditoría privados

    private void auditarAccion(Long usuarioId, String accion, Usuario oldData, Usuario newData) {
        try {
            AuditoriaUsuario auditoria = new AuditoriaUsuario();
            auditoria.setUsuarioId(usuarioId);
            auditoria.setAccion(accion);
            auditoria.setUsername(getCurrentUsername());
            auditoria.setFechaHora(LocalDateTime.now());
            auditoria.setIpAddress(getClientIP());

            if (oldData != null) {
                auditoria.setUsernameAnterior(oldData.getUsername());
                auditoria.setRolAnterior(oldData.getRol().name());
                auditoria.setPasswordAnterior("***");

                // Guardar datos completos como JSON
                try {
                    Map<String, Object> oldMap = new HashMap<>();
                    oldMap.put("id", oldData.getId());
                    oldMap.put("username", oldData.getUsername());
                    oldMap.put("rol", oldData.getRol());
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
                auditoria.setUsernameNuevo(newData.getUsername());
                auditoria.setRolNuevo(newData.getRol().name());
                auditoria.setPasswordCambiada("NO");

                try {
                    Map<String, Object> newMap = new HashMap<>();
                    newMap.put("id", newData.getId());
                    newMap.put("username", newData.getUsername());
                    newMap.put("rol", newData.getRol());
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
            log.debug("Auditoría guardada para usuario {}: {}", usuarioId, accion);
        } catch (Exception e) {
            log.error("Error al guardar auditoría para usuario {}: {}", usuarioId, e.getMessage());
        }
    }

    private void auditarActualizacion(Usuario oldData, Usuario newData, boolean passwordCambiada) {
        try {
            AuditoriaUsuario auditoria = new AuditoriaUsuario();
            auditoria.setUsuarioId(newData.getId());
            auditoria.setAccion("UPDATE");
            auditoria.setUsername(getCurrentUsername());
            auditoria.setFechaHora(LocalDateTime.now());
            auditoria.setIpAddress(getClientIP());

            auditoria.setUsernameAnterior(oldData.getUsername());
            auditoria.setRolAnterior(oldData.getRol().name());
            auditoria.setPasswordAnterior("***");

            auditoria.setUsernameNuevo(newData.getUsername());
            auditoria.setRolNuevo(newData.getRol().name());
            auditoria.setPasswordCambiada(passwordCambiada ? "SI" : "NO");

            try {
                Map<String, Object> oldMap = new HashMap<>();
                oldMap.put("id", oldData.getId());
                oldMap.put("username", oldData.getUsername());
                oldMap.put("rol", oldData.getRol());
                oldMap.put("activo", oldData.getActivo());
                oldMap.put("createdAt", oldData.getCreatedAt());
                oldMap.put("updatedAt", oldData.getUpdatedAt());
                auditoria.setDatosCompletosAnteriores(objectMapper.writeValueAsString(oldMap));

                Map<String, Object> newMap = new HashMap<>();
                newMap.put("id", newData.getId());
                newMap.put("username", newData.getUsername());
                newMap.put("rol", newData.getRol());
                newMap.put("activo", newData.getActivo());
                newMap.put("createdAt", newData.getCreatedAt());
                newMap.put("updatedAt", newData.getUpdatedAt());
                auditoria.setDatosCompletosNuevos(objectMapper.writeValueAsString(newMap));
            } catch (Exception e) {
                log.error("Error al serializar datos para auditoría", e);
            }

            auditoriaRepository.save(auditoria);
            log.debug("Auditoría de actualización guardada para usuario {}", newData.getId());
        } catch (Exception e) {
            log.error("Error al guardar auditoría de actualización", e);
        }
    }

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