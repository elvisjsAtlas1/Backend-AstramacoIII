package com.example.backendastramaco.service.audit;

import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.audit.*;
import com.example.backendastramaco.repository.audit.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditoriaUsuarioRepository auditoriaUsuarioRepo;
    private final AuditoriaTransportistaRepository auditoriaTransportistaRepo;
    private final AuditoriaPedidoRepository auditoriaPedidoRepo;
    private final AuditoriaDocumentoRepository auditoriaDocumentoRepo;
    private final AuditoriaCargaRepository auditoriaCargaRepo;
    private final AuditoriaAuthRepository auditoriaAuthRepo;
    private final ObjectMapper objectMapper;

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            log.warn("No se pudo obtener el usuario actual: {}", e.getMessage());
            return "SYSTEM";
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private String convertToJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Error al convertir objeto a JSON: {}", e.getMessage());
            return "{\"error\": \"Error de serialización\"}";
        }
    }

    // Auditoría para Usuarios
    public void auditUsuario(Long usuarioId, String accion,
                             Object oldData, Object newData,
                             HttpServletRequest request) {
        AuditoriaUsuario audit = new AuditoriaUsuario();
        audit.setUsuarioId(usuarioId);
        audit.setAccion(accion);
        audit.setUsername(getCurrentUsername());
        audit.setFechaHora(LocalDateTime.now());
        audit.setIpAddress(getClientIp(request));
        audit.setDatosCompletosAnteriores(convertToJson(oldData));
        audit.setDatosCompletosNuevos(convertToJson(newData));

        // Mapeo de campos específicos
        mapUsuarioFields(audit, oldData, newData);

        auditoriaUsuarioRepo.save(audit);
    }

    private void mapUsuarioFields(AuditoriaUsuario audit, Object oldData, Object newData) {
        if (oldData instanceof Usuario old) {
            audit.setUsernameAnterior(old.getUsername());
            audit.setRolAnterior(old.getRol() != null ? old.getRol().name() : null);
            // Para password, solo indicamos si cambió (no guardar el valor real)
            if (newData instanceof Usuario newU && old.getPassword() != null && newU.getPassword() != null
                    && !old.getPassword().equals(newU.getPassword())) {
                audit.setPasswordCambiada("SI");
            } else {
                audit.setPasswordCambiada("NO");
            }
        }
        if (newData instanceof Usuario newU) {
            audit.setUsernameNuevo(newU.getUsername());
            audit.setRolNuevo(newU.getRol() != null ? newU.getRol().name() : null);
        }
    }

    // Auditoría para Transportistas
    public void auditTransportista(Long transportistaId, String accion,
                                   Object oldData, Object newData,
                                   HttpServletRequest request) {
        AuditoriaTransportista audit = new AuditoriaTransportista();
        audit.setTransportistaId(transportistaId);
        audit.setAccion(accion);
        audit.setUsername(getCurrentUsername());
        audit.setFechaHora(LocalDateTime.now());
        audit.setIpAddress(getClientIp(request));
        audit.setDatosCompletosAnteriores(convertToJson(oldData));
        audit.setDatosCompletosNuevos(convertToJson(newData));

        // Mapeo de campos específicos para búsquedas rápidas
        mapTransportistaFields(audit, oldData, newData);

        auditoriaTransportistaRepo.save(audit);
        log.debug("Auditoría de transportista registrada - ID: {}, Acción: {}", transportistaId, accion);
    }

    private void mapTransportistaFields(AuditoriaTransportista audit, Object oldData, Object newData) {
        if (oldData instanceof Transportista old) {
            audit.setNombreAnterior(old.getNombre());
            audit.setApellidosAnterior(old.getApellidos());
            audit.setDniAnterior(old.getDni());
            audit.setEdadAnterior(old.getEdad());
            if (old.getTipoTransporte() != null) {
                audit.setTipoTransporteAnterior(old.getTipoTransporte().name());
            }
            audit.setPlacaAnterior(old.getPlaca());
            audit.setVehiculoInfoAnterior(old.getVehiculoInfo());
            audit.setCapacidadAnterior(old.getCapacidad());
            if (old.getEstado() != null) {
                audit.setEstadoAnterior(old.getEstado().name());
            }
        }
        if (newData instanceof Transportista newT) {
            audit.setNombreNuevo(newT.getNombre());
            audit.setApellidosNuevo(newT.getApellidos());
            audit.setDniNuevo(newT.getDni());
            audit.setEdadNuevo(newT.getEdad());
            if (newT.getTipoTransporte() != null) {
                audit.setTipoTransporteNuevo(newT.getTipoTransporte().name());
            }
            audit.setPlacaNuevo(newT.getPlaca());
            audit.setVehiculoInfoNuevo(newT.getVehiculoInfo());
            audit.setCapacidadNuevo(newT.getCapacidad());
            if (newT.getEstado() != null) {
                audit.setEstadoNuevo(newT.getEstado().name());
            }
        }
    }

    // Auditoría para Pedidos
    public void auditPedido(Long pedidoId, String accion,
                            Object oldData, Object newData,
                            HttpServletRequest request) {
        AuditoriaPedido audit = new AuditoriaPedido();
        audit.setPedidoId(pedidoId);
        audit.setAccion(accion);
        audit.setUsername(getCurrentUsername());
        audit.setFechaHora(LocalDateTime.now());
        audit.setIpAddress(getClientIp(request));
        audit.setDatosCompletosAnteriores(convertToJson(oldData));
        audit.setDatosCompletosNuevos(convertToJson(newData));

        auditoriaPedidoRepo.save(audit);
        log.debug("Auditoría de pedido registrada - ID: {}, Acción: {}", pedidoId, accion);
    }

    // Auditoría para Documentos
    public void auditDocumento(Long documentoId, Long transportistaId, String accion,
                               Object oldData, Object newData,
                               HttpServletRequest request) {
        AuditoriaDocumento audit = new AuditoriaDocumento();
        audit.setDocumentoId(documentoId);
        audit.setTransportistaId(transportistaId);
        audit.setAccion(accion);
        audit.setUsername(getCurrentUsername());
        audit.setFechaHora(LocalDateTime.now());
        audit.setIpAddress(getClientIp(request));
        audit.setDatosCompletosAnteriores(convertToJson(oldData));
        audit.setDatosCompletosNuevos(convertToJson(newData));

        auditoriaDocumentoRepo.save(audit);
        log.debug("Auditoría de documento registrada - ID: {}, Acción: {}", documentoId, accion);
    }

    // Auditoría para Cargas
    public void auditCarga(Long cargaId, Long transportistaId, String accion,
                           Object oldData, Object newData,
                           HttpServletRequest request) {
        AuditoriaCarga audit = new AuditoriaCarga();
        audit.setCargaId(cargaId);
        audit.setTransportistaId(transportistaId);
        audit.setAccion(accion);
        audit.setUsername(getCurrentUsername());
        audit.setFechaHora(LocalDateTime.now());
        audit.setIpAddress(getClientIp(request));
        audit.setDatosCompletosAnteriores(convertToJson(oldData));
        audit.setDatosCompletosNuevos(convertToJson(newData));

        auditoriaCargaRepo.save(audit);
        log.debug("Auditoría de carga registrada - ID: {}, Acción: {}", cargaId, accion);
    }

    // Auditoría para Autenticación
    public void auditAuth(String username, String accion, String ipAddress,
                          String userAgent, String mensajeError, Boolean exito) {
        AuditoriaAuth audit = new AuditoriaAuth();
        audit.setUsername(username);
        audit.setAccion(accion);
        audit.setIpAddress(ipAddress);
        audit.setUserAgent(userAgent);
        audit.setMensajeError(mensajeError);
        audit.setExito(exito);
        audit.setFechaHora(LocalDateTime.now());

        auditoriaAuthRepo.save(audit);
        log.debug("Auditoría de autenticación registrada - Usuario: {}, Acción: {}, Éxito: {}",
                username, accion, exito);
    }
}