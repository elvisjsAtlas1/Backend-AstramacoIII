package com.example.backendastramaco.unitarias;

import com.example.backendastramaco.service.audit.AuditService;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.audit.*;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.model.enums.EstadoTransportista;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.audit.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceUnitTest {

    @Mock
    private AuditoriaUsuarioRepository auditoriaUsuarioRepo;
    @Mock
    private AuditoriaTransportistaRepository auditoriaTransportistaRepo;
    @Mock
    private AuditoriaPedidoRepository auditoriaPedidoRepo;
    @Mock
    private AuditoriaDocumentoRepository auditoriaDocumentoRepo;
    @Mock
    private AuditoriaCargaRepository auditoriaCargaRepo;
    @Mock
    private AuditoriaAuthRepository auditoriaAuthRepo;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        // Configuramos un contexto de seguridad simulado por defecto
        SecurityContextHolder.setContext(securityContext);
    }

    // Helper para simular usuario autenticado
    private void mockAuthenticatedUser(String username) {
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn(username);
    }

    // Helper para simular IP
    private void mockClientIp(String ip, String xForwardedFor) {
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(xForwardedFor);
        lenient().when(request.getRemoteAddr()).thenReturn(ip);
    }

    @Test
    @DisplayName("Debe auditar Usuario correctamente extrayendo IP y Username")
    void auditUsuario_DebeGuardarAuditoriaCorrectamente() throws Exception {
        // Arrange
        mockAuthenticatedUser("admin.test");
        mockClientIp("192.168.1.100", null);

        Usuario oldData = new Usuario();
        oldData.setUsername("old.user");
        oldData.setRol(Rol.USER);
        oldData.setPassword("oldPass");

        Usuario newData = new Usuario();
        newData.setUsername("new.user");
        newData.setRol(Rol.ADMIN);
        newData.setPassword("newPass"); // Contraseña diferente

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"json\":\"data\"}");

        // Act
        auditService.auditUsuario(1L, "UPDATE", oldData, newData, request);

        // Assert
        ArgumentCaptor<AuditoriaUsuario> captor = ArgumentCaptor.forClass(AuditoriaUsuario.class);
        verify(auditoriaUsuarioRepo).save(captor.capture());
        AuditoriaUsuario savedAudit = captor.getValue();

        assertThat(savedAudit.getUsuarioId()).isEqualTo(1L);
        assertThat(savedAudit.getAccion()).isEqualTo("UPDATE");
        assertThat(savedAudit.getUsername()).isEqualTo("admin.test");
        assertThat(savedAudit.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(savedAudit.getUsernameAnterior()).isEqualTo("old.user");
        assertThat(savedAudit.getUsernameNuevo()).isEqualTo("new.user");
        assertThat(savedAudit.getRolAnterior()).isEqualTo("USER");
        assertThat(savedAudit.getRolNuevo()).isEqualTo("ADMIN");
        assertThat(savedAudit.getPasswordCambiada()).isEqualTo("SI");
    }

    @Test
    @DisplayName("Debe auditar Transportista extrayendo IP de X-Forwarded-For")
    void auditTransportista_DebeUsarXForwardedFor() throws Exception {
        // Arrange
        mockAuthenticatedUser("admin.test");
        mockClientIp("10.0.0.1", "203.0.113.195, 10.0.0.1"); // Simula proxy

        Transportista oldData = new Transportista();
        oldData.setNombre("Carlos");
        oldData.setTipoTransporte(TipoTransporte.CAMIONERO);
        oldData.setEstado(EstadoTransportista.ACTIVO);

        Transportista newData = new Transportista();
        newData.setNombre("Carlos Modificado");

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        auditService.auditTransportista(5L, "UPDATE", oldData, newData, request);

        // Assert
        ArgumentCaptor<AuditoriaTransportista> captor = ArgumentCaptor.forClass(AuditoriaTransportista.class);
        verify(auditoriaTransportistaRepo).save(captor.capture());
        AuditoriaTransportista savedAudit = captor.getValue();

        assertThat(savedAudit.getTransportistaId()).isEqualTo(5L);
        assertThat(savedAudit.getIpAddress()).isEqualTo("203.0.113.195"); // Tomó la primera IP
        assertThat(savedAudit.getNombreAnterior()).isEqualTo("Carlos");
        assertThat(savedAudit.getNombreNuevo()).isEqualTo("Carlos Modificado");
        assertThat(savedAudit.getTipoTransporteAnterior()).isEqualTo("CAMIONERO");
        assertThat(savedAudit.getEstadoAnterior()).isEqualTo("ACTIVO");
    }

    @Test
    @DisplayName("Debe manejar fallos de autenticación (SecurityContext nulo) y asignar 'SYSTEM'")
    void getCurrentUsername_DebeAsignarSystemCuandoFallaAuth() {
        // Arrange
        when(securityContext.getAuthentication()).thenThrow(new RuntimeException("No auth context"));

        // Act
        auditService.auditPedido(10L, "CREATE", null, null, null);

        // Assert
        ArgumentCaptor<AuditoriaPedido> captor = ArgumentCaptor.forClass(AuditoriaPedido.class);
        verify(auditoriaPedidoRepo).save(captor.capture());

        assertThat(captor.getValue().getUsername()).isEqualTo("SYSTEM");
        assertThat(captor.getValue().getIpAddress()).isEqualTo("0.0.0.0"); // Porque el request es null
    }

    @Test
    @DisplayName("Debe manejar errores de serialización JSON limpiamente")
    void convertToJson_DebeManejarExcepcion() throws Exception {
        // Arrange
        mockAuthenticatedUser("system");
        Object badObject = new Object();
        when(objectMapper.writeValueAsString(badObject)).thenThrow(new RuntimeException("Serialization error"));

        // Act
        auditService.auditDocumento(1L, 2L, "CREATE", badObject, null, null);

        // Assert
        ArgumentCaptor<AuditoriaDocumento> captor = ArgumentCaptor.forClass(AuditoriaDocumento.class);
        verify(auditoriaDocumentoRepo).save(captor.capture());

        assertThat(captor.getValue().getDatosCompletosAnteriores()).contains("Error de serialización");
        assertThat(captor.getValue().getDatosCompletosNuevos()).isNull();
    }

    @Test
    @DisplayName("Debe auditar Cargas correctamente")
    void auditCarga_DebeGuardarCorrectamente() {
        // Arrange
        mockAuthenticatedUser("usuario.test");

        // Act
        auditService.auditCarga(100L, 50L, "DELETE", null, null, null);

        // Assert
        ArgumentCaptor<AuditoriaCarga> captor = ArgumentCaptor.forClass(AuditoriaCarga.class);
        verify(auditoriaCargaRepo).save(captor.capture());

        assertThat(captor.getValue().getCargaId()).isEqualTo(100L);
        assertThat(captor.getValue().getTransportistaId()).isEqualTo(50L);
        assertThat(captor.getValue().getAccion()).isEqualTo("DELETE");
    }

    @Test
    @DisplayName("Debe auditar Autenticación (Login) correctamente")
    void auditAuth_DebeGuardarCorrectamente() {
        // Act
        auditService.auditAuth("test.login", "LOGIN_FAILED", "127.0.0.1", "Mozilla/5.0", "Bad credentials", false);

        // Assert
        ArgumentCaptor<AuditoriaAuth> captor = ArgumentCaptor.forClass(AuditoriaAuth.class);
        verify(auditoriaAuthRepo).save(captor.capture());

        assertThat(captor.getValue().getUsername()).isEqualTo("test.login");
        assertThat(captor.getValue().getAccion()).isEqualTo("LOGIN_FAILED");
        assertThat(captor.getValue().getExito()).isFalse();
        assertThat(captor.getValue().getMensajeError()).isEqualTo("Bad credentials");
    }
}