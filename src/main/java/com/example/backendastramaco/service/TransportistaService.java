package com.example.backendastramaco.service;

import com.example.backendastramaco.dto.TransportistaRequestDTO;
import com.example.backendastramaco.exception.DuplicateResourceException;
import com.example.backendastramaco.exception.ResourceNotFoundException;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.audit.AuditoriaTransportista;
import com.example.backendastramaco.model.enums.EstadoTransportista;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.repository.audit.AuditoriaTransportistaRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransportistaService {

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
            throw new DuplicateResourceException("El DNI '" + dto.getDni() + "' ya está registrado");
        }

        // Validar placa única
        if (transportistaRepository.existsByPlaca(dto.getPlaca())) {
            throw new DuplicateResourceException("La placa '" + dto.getPlaca() + "' ya está registrada");
        }

        // ✅ CREAR USUARIO AUTOMÁTICAMENTE
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
            t.setCapacidad(0.0);
        } else {
            t.setCapacidad(dto.getCapacidad());
        }

        t.setEstado(EstadoTransportista.ACTIVO);

        Transportista saved = transportistaRepository.save(t);
        log.info("Transportista creado con ID: {}, Usuario creado: {}", saved.getId(), username);

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
                .orElseThrow(() -> new ResourceNotFoundException("Transportista no encontrado con ID: " + id));
    }

    @Transactional(readOnly = true)
    public Transportista obtenerPorDni(String dni) {
        log.debug("Obteniendo transportista por DNI: {}", dni);
        return transportistaRepository.findByDniAndDeletedAtIsNull(dni)
                .orElseThrow(() -> new ResourceNotFoundException("Transportista no encontrado con DNI: " + dni));
    }

    @Transactional(readOnly = true)
    public Transportista obtenerPorUsuarioId(Long usuarioId) {
        log.debug("Obteniendo transportista por usuario ID: {}", usuarioId);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));
        return transportistaRepository.findByUsuarioAndDeletedAtIsNull(usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Transportista no encontrado para el usuario: " + usuarioId));
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
                .orElseThrow(() -> new ResourceNotFoundException("Transportista no encontrado con ID: " + id));

        // Validar DNI único (excepto el mismo)
        if (!existente.getDni().equals(dto.getDni()) &&
                transportistaRepository.existsByDni(dto.getDni())) {
            throw new DuplicateResourceException("El DNI '" + dto.getDni() + "' ya está registrado");
        }

        // Validar placa única (excepto el mismo)
        if (!existente.getPlaca().equals(dto.getPlaca()) &&
                transportistaRepository.existsByPlaca(dto.getPlaca())) {
            throw new DuplicateResourceException("La placa '" + dto.getPlaca() + "' ya está registrada");
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

        // ✅ Si es CAMIONERO, capacidad es 0 (no aplica)
        if (dto.getTipoTransporte() == TipoTransporte.CAMIONERO) {
            existente.setCapacidad(0.0);
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
                .orElseThrow(() -> new ResourceNotFoundException("Transportista no encontrado con ID: " + id));

        Transportista oldCopy = copiarEntidad(transportista);
        transportista.setEstado(EstadoTransportista.valueOf(estado.toUpperCase()));
        transportistaRepository.save(transportista);

        // Auditar cambio de estado
        auditarAccion(
                id,
                "UPDATE_ESTADO",
                oldCopy,
                transportista
        );

        log.info("Estado del transportista {} cambiado a: {}", id, estado);
    }

    @Transactional
    public void eliminar(Long id, String username) {
        log.info("Eliminando (soft delete) transportista con ID: {}", id);

        Transportista existente = transportistaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transportista no encontrado con ID: " + id));

        if (existente.getDeletedAt() != null) {
            throw new RuntimeException("El transportista ya está eliminado");
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
                "DELETE",
                oldCopy,
                null
        );

        log.info("Transportista {} eliminado (soft delete) por: {}", id, username);
    }

    @Transactional
    public void restaurar(Long id) {
        log.info("Restaurando transportista con ID: {}", id);

        Transportista transportista = transportistaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transportista no encontrado con ID: " + id));

        if (transportista.getDeletedAt() == null) {
            throw new RuntimeException("El transportista no está eliminado");
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
                "RESTORE",
                oldCopy,
                transportista
        );

        log.info("Transportista {} restaurado", id);
    }

    @Transactional
    public void eliminarPermanente(Long id) {
        log.info("Eliminando permanentemente transportista con ID: {}", id);

        Transportista transportista = transportistaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transportista no encontrado con ID: " + id));

        // Guardar copia para auditoría antes de eliminar
        Transportista oldCopy = copiarEntidad(transportista);

        // Auditar eliminación permanente
        auditarAccion(
                id,
                "DELETE_PERMANENT",
                oldCopy,
                null
        );

        transportistaRepository.delete(transportista);
        log.info("Transportista {} eliminado permanentemente", id);
    }

    // Métodos de auditoría privados

    private void auditarAccion(Long transportistaId, String accion, Transportista oldData, Transportista newData) {
        try {
            AuditoriaTransportista auditoria = new AuditoriaTransportista();
            auditoria.setTransportistaId(transportistaId);
            auditoria.setAccion(accion);
            auditoria.setUsername(getCurrentUsername());
            auditoria.setFechaHora(LocalDateTime.now());
            auditoria.setIpAddress(getClientIP());

            if (oldData != null) {
                auditoria.setNombreAnterior(oldData.getNombre());
                auditoria.setApellidosAnterior(oldData.getApellidos());
                auditoria.setDniAnterior(oldData.getDni());
                auditoria.setEdadAnterior(oldData.getEdad());
                auditoria.setTipoTransporteAnterior(oldData.getTipoTransporte() != null ? oldData.getTipoTransporte().name() : null);
                auditoria.setPlacaAnterior(oldData.getPlaca());
                auditoria.setVehiculoInfoAnterior(oldData.getVehiculoInfo());
                auditoria.setCapacidadAnterior(oldData.getCapacidad());
                auditoria.setEstadoAnterior(oldData.getEstado() != null ? oldData.getEstado().name() : null);

                try {
                    Map<String, Object> oldMap = new HashMap<>();
                    oldMap.put("id", oldData.getId());
                    oldMap.put("nombre", oldData.getNombre());
                    oldMap.put("apellidos", oldData.getApellidos());
                    oldMap.put("dni", oldData.getDni());
                    oldMap.put("edad", oldData.getEdad());
                    oldMap.put("tipoTransporte", oldData.getTipoTransporte());
                    oldMap.put("placa", oldData.getPlaca());
                    oldMap.put("vehiculoInfo", oldData.getVehiculoInfo());
                    oldMap.put("capacidad", oldData.getCapacidad());
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
                auditoria.setNombreNuevo(newData.getNombre());
                auditoria.setApellidosNuevo(newData.getApellidos());
                auditoria.setDniNuevo(newData.getDni());
                auditoria.setEdadNuevo(newData.getEdad());
                auditoria.setTipoTransporteNuevo(newData.getTipoTransporte() != null ? newData.getTipoTransporte().name() : null);
                auditoria.setPlacaNuevo(newData.getPlaca());
                auditoria.setVehiculoInfoNuevo(newData.getVehiculoInfo());
                auditoria.setCapacidadNuevo(newData.getCapacidad());
                auditoria.setEstadoNuevo(newData.getEstado() != null ? newData.getEstado().name() : null);

                try {
                    Map<String, Object> newMap = new HashMap<>();
                    newMap.put("id", newData.getId());
                    newMap.put("nombre", newData.getNombre());
                    newMap.put("apellidos", newData.getApellidos());
                    newMap.put("dni", newData.getDni());
                    newMap.put("edad", newData.getEdad());
                    newMap.put("tipoTransporte", newData.getTipoTransporte());
                    newMap.put("placa", newData.getPlaca());
                    newMap.put("vehiculoInfo", newData.getVehiculoInfo());
                    newMap.put("capacidad", newData.getCapacidad());
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
            log.debug("Auditoría guardada para transportista {}: {}", transportistaId, accion);
        } catch (Exception e) {
            log.error("Error al guardar auditoría para transportista {}: {}", transportistaId, e.getMessage());
        }
    }

    private void auditarActualizacion(Transportista oldData, Transportista newData) {
        try {
            AuditoriaTransportista auditoria = new AuditoriaTransportista();
            auditoria.setTransportistaId(newData.getId());
            auditoria.setAccion("UPDATE");
            auditoria.setUsername(getCurrentUsername());
            auditoria.setFechaHora(LocalDateTime.now());
            auditoria.setIpAddress(getClientIP());

            // Datos anteriores
            auditoria.setNombreAnterior(oldData.getNombre());
            auditoria.setApellidosAnterior(oldData.getApellidos());
            auditoria.setDniAnterior(oldData.getDni());
            auditoria.setEdadAnterior(oldData.getEdad());
            auditoria.setTipoTransporteAnterior(oldData.getTipoTransporte() != null ? oldData.getTipoTransporte().name() : null);
            auditoria.setPlacaAnterior(oldData.getPlaca());
            auditoria.setVehiculoInfoAnterior(oldData.getVehiculoInfo());
            auditoria.setCapacidadAnterior(oldData.getCapacidad());
            auditoria.setEstadoAnterior(oldData.getEstado() != null ? oldData.getEstado().name() : null);

            // Datos nuevos
            auditoria.setNombreNuevo(newData.getNombre());
            auditoria.setApellidosNuevo(newData.getApellidos());
            auditoria.setDniNuevo(newData.getDni());
            auditoria.setEdadNuevo(newData.getEdad());
            auditoria.setTipoTransporteNuevo(newData.getTipoTransporte() != null ? newData.getTipoTransporte().name() : null);
            auditoria.setPlacaNuevo(newData.getPlaca());
            auditoria.setVehiculoInfoNuevo(newData.getVehiculoInfo());
            auditoria.setCapacidadNuevo(newData.getCapacidad());
            auditoria.setEstadoNuevo(newData.getEstado() != null ? newData.getEstado().name() : null);

            try {
                Map<String, Object> oldMap = new HashMap<>();
                oldMap.put("id", oldData.getId());
                oldMap.put("nombre", oldData.getNombre());
                oldMap.put("apellidos", oldData.getApellidos());
                oldMap.put("dni", oldData.getDni());
                oldMap.put("edad", oldData.getEdad());
                oldMap.put("tipoTransporte", oldData.getTipoTransporte());
                oldMap.put("placa", oldData.getPlaca());
                oldMap.put("vehiculoInfo", oldData.getVehiculoInfo());
                oldMap.put("capacidad", oldData.getCapacidad());
                oldMap.put("estado", oldData.getEstado());
                oldMap.put("createdAt", oldData.getCreatedAt());
                oldMap.put("updatedAt", oldData.getUpdatedAt());
                auditoria.setDatosCompletosAnteriores(objectMapper.writeValueAsString(oldMap));

                Map<String, Object> newMap = new HashMap<>();
                newMap.put("id", newData.getId());
                newMap.put("nombre", newData.getNombre());
                newMap.put("apellidos", newData.getApellidos());
                newMap.put("dni", newData.getDni());
                newMap.put("edad", newData.getEdad());
                newMap.put("tipoTransporte", newData.getTipoTransporte());
                newMap.put("placa", newData.getPlaca());
                newMap.put("vehiculoInfo", newData.getVehiculoInfo());
                newMap.put("capacidad", newData.getCapacidad());
                newMap.put("estado", newData.getEstado());
                newMap.put("createdAt", newData.getCreatedAt());
                newMap.put("updatedAt", newData.getUpdatedAt());
                auditoria.setDatosCompletosNuevos(objectMapper.writeValueAsString(newMap));
            } catch (Exception e) {
                log.error("Error al serializar datos para auditoría", e);
            }

            auditoriaRepository.save(auditoria);
            log.debug("Auditoría de actualización guardada para transportista {}", newData.getId());
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
        return primerNombre + "." + primerApellido;
    }
}