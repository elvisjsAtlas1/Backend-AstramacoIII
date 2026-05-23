package com.example.backendastramaco.service.audit;

import com.example.backendastramaco.model.audit.*;
import com.example.backendastramaco.repository.audit.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

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
            return "SYSTEM";
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "0.0.0.0";
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) return request.getRemoteAddr();
        return xfHeader.split(",")[0];
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

        if (oldData != null) {
            try {
                audit.setDatosCompletosAnteriores(objectMapper.writeValueAsString(oldData));
            } catch (Exception e) {}
        }
        if (newData != null) {
            try {
                audit.setDatosCompletosNuevos(objectMapper.writeValueAsString(newData));
            } catch (Exception e) {}
        }

        auditoriaUsuarioRepo.save(audit);
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

        if (oldData != null) {
            try {
                audit.setDatosCompletosAnteriores(objectMapper.writeValueAsString(oldData));
            } catch (Exception e) {}
        }
        if (newData != null) {
            try {
                audit.setDatosCompletosNuevos(objectMapper.writeValueAsString(newData));
            } catch (Exception e) {}
        }

        auditoriaTransportistaRepo.save(audit);
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

        if (oldData != null) {
            try {
                audit.setDatosCompletosAnteriores(objectMapper.writeValueAsString(oldData));
            } catch (Exception e) {}
        }
        if (newData != null) {
            try {
                audit.setDatosCompletosNuevos(objectMapper.writeValueAsString(newData));
            } catch (Exception e) {}
        }

        auditoriaPedidoRepo.save(audit);
    }

    // Auditoría para Documentos
    public void auditDocumento(Long documentoId, Long transportistaId, String accion,
                               Object oldData, Object newData,
                               HttpServletRequest request) {
        AuditoriaDocumentoPersonal audit = new AuditoriaDocumentoPersonal();
        audit.setDocumentoId(documentoId);
        audit.setTransportistaId(transportistaId);
        audit.setAccion(accion);
        audit.setUsername(getCurrentUsername());
        audit.setFechaHora(LocalDateTime.now());
        audit.setIpAddress(getClientIp(request));

        if (oldData != null) {
            try {
                audit.setDatosCompletosAnteriores(objectMapper.writeValueAsString(oldData));
            } catch (Exception e) {}
        }
        if (newData != null) {
            try {
                audit.setDatosCompletosNuevos(objectMapper.writeValueAsString(newData));
            } catch (Exception e) {}
        }

        auditoriaDocumentoRepo.save(audit);
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

        if (oldData != null) {
            try {
                audit.setDatosCompletosAnteriores(objectMapper.writeValueAsString(oldData));
            } catch (Exception e) {}
        }
        if (newData != null) {
            try {
                audit.setDatosCompletosNuevos(objectMapper.writeValueAsString(newData));
            } catch (Exception e) {}
        }

        auditoriaCargaRepo.save(audit);
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
    }
}