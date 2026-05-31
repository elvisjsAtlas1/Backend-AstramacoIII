package com.example.backendastramaco;

import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.audit.*;
import com.example.backendastramaco.model.enums.EstadoTransportista;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.audit.*;
import com.example.backendastramaco.service.audit.AuditService;
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

import static org.junit.jupiter.api.Assertions.*;
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

    @InjectMocks
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        // Limpiar contexto de seguridad antes de cada prueba
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Debe auditar creación de usuario correctamente")
    void auditUsuario_DebeGuardarAuditoriaDeUsuario() throws Exception {
        // Arrange
        Long usuarioId = 1L;
        String accion = "CREATE";
        Usuario oldData = null;
        Usuario newData = new Usuario();
        newData.setUsername("admin");
        newData.setRol(Rol.ADMIN);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        mockAuthentication("adminUser");

        when(objectMapper.writeValueAsString(newData)).thenReturn("{\"username\":\"admin\"}");


        ArgumentCaptor<AuditoriaUsuario> captor = ArgumentCaptor.forClass(AuditoriaUsuario.class);
        when(auditoriaUsuarioRepo.save(any(AuditoriaUsuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditService.auditUsuario(usuarioId, accion, oldData, newData, request);

        // Assert
        verify(auditoriaUsuarioRepo).save(captor.capture());
        AuditoriaUsuario audit = captor.getValue();

        assertEquals(usuarioId, audit.getUsuarioId());
        assertEquals(accion, audit.getAccion());
        assertEquals("adminUser", audit.getUsername());
        assertEquals("127.0.0.1", audit.getIpAddress());
        assertNotNull(audit.getFechaHora());

        verify(auditoriaUsuarioRepo, times(1)).save(any(AuditoriaUsuario.class));
    }

    @Test
    @DisplayName("Debe auditar actualización de transportista con mapeo de campos específicos")
    void auditTransportista_DebeGuardarAuditoriaConCamposMapeados() throws Exception {
        // Arrange
        Long transportistaId = 1L;
        String accion = "UPDATE";

        Transportista oldData = new Transportista();
        oldData.setNombre("Juan");
        oldData.setApellidos("Perez");
        oldData.setDni("12345678");
        oldData.setEdad(30);
        oldData.setTipoTransporte(TipoTransporte.CAMIONERO);
        oldData.setPlaca("ABC-123");
        oldData.setVehiculoInfo("Camion rojo");
        oldData.setCapacidad(10.5);
        oldData.setEstado(EstadoTransportista.ACTIVO);

        Transportista newData = new Transportista();
        newData.setNombre("Juan Carlos");
        newData.setApellidos("Perez Gomez");
        newData.setDni("87654321");
        newData.setEdad(35);
        newData.setTipoTransporte(TipoTransporte.VOLQUETERO);
        newData.setPlaca("XYZ-789");
        newData.setVehiculoInfo("Camion azul");
        newData.setCapacidad(15.0);
        newData.setEstado(EstadoTransportista.INACTIVO);

        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        mockAuthentication("transportistaAdmin");

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        ArgumentCaptor<AuditoriaTransportista> captor = ArgumentCaptor.forClass(AuditoriaTransportista.class);
        when(auditoriaTransportistaRepo.save(any(AuditoriaTransportista.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditService.auditTransportista(transportistaId, accion, oldData, newData, request);

        // Assert
        verify(auditoriaTransportistaRepo).save(captor.capture());
        AuditoriaTransportista audit = captor.getValue();

        assertEquals(transportistaId, audit.getTransportistaId());
        assertEquals(accion, audit.getAccion());

        // Verificar campos anteriores
        assertEquals("Juan", audit.getNombreAnterior());
        assertEquals("Perez", audit.getApellidosAnterior());
        assertEquals("12345678", audit.getDniAnterior());
        assertEquals(30, audit.getEdadAnterior());
        assertEquals(TipoTransporte.CAMIONERO.name(), audit.getTipoTransporteAnterior());
        assertEquals("ABC-123", audit.getPlacaAnterior());
        assertEquals("Camion rojo", audit.getVehiculoInfoAnterior());
        assertEquals(10.5, audit.getCapacidadAnterior());
        assertEquals(EstadoTransportista.ACTIVO.name(), audit.getEstadoAnterior());

        // Verificar campos nuevos
        assertEquals("Juan Carlos", audit.getNombreNuevo());
        assertEquals("Perez Gomez", audit.getApellidosNuevo());
        assertEquals("87654321", audit.getDniNuevo());
        assertEquals(35, audit.getEdadNuevo());
        assertEquals(TipoTransporte.VOLQUETERO.name(), audit.getTipoTransporteNuevo());
        assertEquals("XYZ-789", audit.getPlacaNuevo());
        assertEquals("Camion azul", audit.getVehiculoInfoNuevo());
        assertEquals(15.0, audit.getCapacidadNuevo());
        assertEquals(EstadoTransportista.INACTIVO.name(), audit.getEstadoNuevo());
    }

    @Test
    @DisplayName("Debe auditar pedido correctamente")
    void auditPedido_DebeGuardarAuditoriaDePedido() throws Exception {
        // Arrange
        Long pedidoId = 1L;
        String accion = "CREATE";
        Object oldData = null;
        Object newData = "Pedido creado";

        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        mockAuthentication("clienteUser");

        when(objectMapper.writeValueAsString(newData)).thenReturn("{\"data\":\"Pedido creado\"}");

        ArgumentCaptor<AuditoriaPedido> captor = ArgumentCaptor.forClass(AuditoriaPedido.class);
        when(auditoriaPedidoRepo.save(any(AuditoriaPedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditService.auditPedido(pedidoId, accion, oldData, newData, request);

        // Assert
        verify(auditoriaPedidoRepo).save(captor.capture());
        AuditoriaPedido audit = captor.getValue();

        assertEquals(pedidoId, audit.getPedidoId());
        assertEquals(accion, audit.getAccion());
        assertEquals("clienteUser", audit.getUsername());
        assertEquals("10.0.0.1", audit.getIpAddress());

        verify(auditoriaPedidoRepo, times(1)).save(any(AuditoriaPedido.class));
    }

    @Test
    @DisplayName("Debe auditar documento correctamente")
    void auditDocumento_DebeGuardarAuditoriaDeDocumento() throws Exception {
        // Arrange
        Long documentoId = 1L;
        Long transportistaId = 2L;
        String accion = "UPDATE";
        Object oldData = "Documento anterior";
        Object newData = "Documento nuevo";

        when(request.getRemoteAddr()).thenReturn("172.16.0.1");
        mockAuthentication("adminUser");

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        ArgumentCaptor<AuditoriaDocumentoPersonal> captor = ArgumentCaptor.forClass(AuditoriaDocumentoPersonal.class);
        when(auditoriaDocumentoRepo.save(any(AuditoriaDocumentoPersonal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditService.auditDocumento(documentoId, transportistaId, accion, oldData, newData, request);

        // Assert
        verify(auditoriaDocumentoRepo).save(captor.capture());
        AuditoriaDocumentoPersonal audit = captor.getValue();

        assertEquals(documentoId, audit.getDocumentoId());
        assertEquals(transportistaId, audit.getTransportistaId());
        assertEquals(accion, audit.getAccion());
        assertEquals("adminUser", audit.getUsername());
        assertEquals("172.16.0.1", audit.getIpAddress());

        verify(auditoriaDocumentoRepo, times(1)).save(any(AuditoriaDocumentoPersonal.class));
    }

    @Test
    @DisplayName("Debe auditar carga correctamente")
    void auditCarga_DebeGuardarAuditoriaDeCarga() throws Exception {
        // Arrange
        Long cargaId = 1L;
        Long transportistaId = 2L;
        String accion = "UPDATE";
        Object oldData = "Carga anterior";
        Object newData = "Carga nueva";

        when(request.getRemoteAddr()).thenReturn("192.168.1.50");
        mockAuthentication("transportistaUser");

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        ArgumentCaptor<AuditoriaCarga> captor = ArgumentCaptor.forClass(AuditoriaCarga.class);
        when(auditoriaCargaRepo.save(any(AuditoriaCarga.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditService.auditCarga(cargaId, transportistaId, accion, oldData, newData, request);

        // Assert
        verify(auditoriaCargaRepo).save(captor.capture());
        AuditoriaCarga audit = captor.getValue();

        assertEquals(cargaId, audit.getCargaId());
        assertEquals(transportistaId, audit.getTransportistaId());
        assertEquals(accion, audit.getAccion());
        assertEquals("transportistaUser", audit.getUsername());
        assertEquals("192.168.1.50", audit.getIpAddress());

        verify(auditoriaCargaRepo, times(1)).save(any(AuditoriaCarga.class));
    }

    @Test
    @DisplayName("Debe auditar autenticación exitosa")
    void auditAuth_DebeGuardarAuditoriaAutenticacionExitosa() {
        // Arrange
        String username = "admin";
        String accion = "LOGIN";
        String ipAddress = "192.168.1.100";
        String userAgent = "Mozilla/5.0";
        String mensajeError = null;
        Boolean exito = true;

        ArgumentCaptor<AuditoriaAuth> captor = ArgumentCaptor.forClass(AuditoriaAuth.class);
        when(auditoriaAuthRepo.save(any(AuditoriaAuth.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditService.auditAuth(username, accion, ipAddress, userAgent, mensajeError, exito);

        // Assert
        verify(auditoriaAuthRepo).save(captor.capture());
        AuditoriaAuth audit = captor.getValue();

        assertEquals(username, audit.getUsername());
        assertEquals(accion, audit.getAccion());
        assertEquals(ipAddress, audit.getIpAddress());
        assertEquals(userAgent, audit.getUserAgent());
        assertNull(audit.getMensajeError());
        assertTrue(audit.getExito());
        assertNotNull(audit.getFechaHora());

        verify(auditoriaAuthRepo, times(1)).save(any(AuditoriaAuth.class));
    }

    @Test
    @DisplayName("Debe auditar autenticación fallida con mensaje de error")
    void auditAuth_DebeGuardarAuditoriaAutenticacionFallida() {
        // Arrange
        String username = "usuario_invalido";
        String accion = "LOGIN";
        String ipAddress = "10.0.0.5";
        String userAgent = "Chrome";
        String mensajeError = "Credenciales inválidas";
        Boolean exito = false;

        ArgumentCaptor<AuditoriaAuth> captor = ArgumentCaptor.forClass(AuditoriaAuth.class);
        when(auditoriaAuthRepo.save(any(AuditoriaAuth.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditService.auditAuth(username, accion, ipAddress, userAgent, mensajeError, exito);

        // Assert
        verify(auditoriaAuthRepo).save(captor.capture());
        AuditoriaAuth audit = captor.getValue();

        assertEquals(username, audit.getUsername());
        assertEquals(accion, audit.getAccion());
        assertEquals(ipAddress, audit.getIpAddress());
        assertEquals(userAgent, audit.getUserAgent());
        assertEquals(mensajeError, audit.getMensajeError());
        assertFalse(audit.getExito());

        verify(auditoriaAuthRepo, times(1)).save(any(AuditoriaAuth.class));
    }

    @Test
    @DisplayName("Debe obtener IP del cliente desde X-Forwarded-For cuando está presente")
    void getClientIp_DebeUsarXForwardedFor_CuandoExiste() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 198.51.100.7");

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auditoriaDocumentoRepo.save(any(AuditoriaDocumentoPersonal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockAuthentication("testUser");

        // Act
        auditService.auditDocumento(1L, 1L, "CREATE", null, "data", request);

        // Assert
        ArgumentCaptor<AuditoriaDocumentoPersonal> captor = ArgumentCaptor.forClass(AuditoriaDocumentoPersonal.class);
        verify(auditoriaDocumentoRepo).save(captor.capture());
        assertEquals("203.0.113.5", captor.getValue().getIpAddress());
    }

    @Test
    @DisplayName("Debe usar remoteAddr cuando no hay X-Forwarded-For")
    void getClientIp_DebeUsarRemoteAddr_CuandoNoHayXForwardedFor() throws Exception {
        // Arrange
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auditoriaDocumentoRepo.save(any(AuditoriaDocumentoPersonal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockAuthentication("testUser");

        // Act
        auditService.auditDocumento(1L, 1L, "CREATE", null, "data", request);

        // Assert
        ArgumentCaptor<AuditoriaDocumentoPersonal> captor = ArgumentCaptor.forClass(AuditoriaDocumentoPersonal.class);
        verify(auditoriaDocumentoRepo).save(captor.capture());
        assertEquals("192.168.1.100", captor.getValue().getIpAddress());
    }

    @Test
    @DisplayName("Debe usar SYSTEM cuando no hay autenticación")
    void getCurrentUsername_DebeUsarSystem_CuandoNoHayAutenticacion() throws Exception {
        // Arrange
        SecurityContextHolder.clearContext();
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auditoriaUsuarioRepo.save(any(AuditoriaUsuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditService.auditUsuario(1L, "CREATE", null, new Usuario(), request);

        // Assert
        ArgumentCaptor<AuditoriaUsuario> captor = ArgumentCaptor.forClass(AuditoriaUsuario.class);
        verify(auditoriaUsuarioRepo).save(captor.capture());
        assertEquals("SYSTEM", captor.getValue().getUsername());
    }

    @Test
    @DisplayName("Debe manejar request null para IP")
    void auditUsuario_DebeManejarRequestNull() throws Exception {
        // Arrange
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(auditoriaUsuarioRepo.save(any(AuditoriaUsuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockAuthentication("testUser");

        // Act
        auditService.auditUsuario(1L, "CREATE", null, new Usuario(), null);

        // Assert
        ArgumentCaptor<AuditoriaUsuario> captor = ArgumentCaptor.forClass(AuditoriaUsuario.class);
        verify(auditoriaUsuarioRepo).save(captor.capture());
        assertEquals("0.0.0.0", captor.getValue().getIpAddress());
    }

    // Helper method para mockear autenticación
    private void mockAuthentication(String username) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}