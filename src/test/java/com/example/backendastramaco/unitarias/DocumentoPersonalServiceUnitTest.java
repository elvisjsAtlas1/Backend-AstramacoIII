package com.example.backendastramaco.unitarias;

import com.example.backendastramaco.exception.DuplicateResourceException;
import com.example.backendastramaco.exception.ResourceNotFoundException;
import com.example.backendastramaco.model.DocumentoPersonal;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.audit.AuditoriaDocumento;
import com.example.backendastramaco.model.enums.TipoDocumento;
import com.example.backendastramaco.repository.DocumentoPersonalRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.audit.AuditoriaDocumentoRepository;
import com.example.backendastramaco.service.DocumentoPersonalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoPersonalServiceUnitTest {

    @Mock
    private DocumentoPersonalRepository documentoRepository;

    @Mock
    private TransportistaRepository transportistaRepository;

    @Mock
    private AuditoriaDocumentoRepository auditoriaRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private DocumentoPersonalService documentoService;

    private DocumentoPersonal documento;
    private Transportista transportista;

    @BeforeEach
    void setUp() {
        // Configurar transportista base
        transportista = new Transportista();
        ReflectionTestUtils.setField(transportista, "id", 1L);

        // Configurar documento base
        documento = new DocumentoPersonal();
        ReflectionTestUtils.setField(documento, "id", 1L);
        documento.setTipoDocumento(TipoDocumento.SOAT);
        documento.setValor("Vigente");
        documento.setFechaEmision(LocalDate.of(2025, 1, 1));
        documento.setFechaVencimiento(LocalDate.of(2026, 12, 31));
        documento.setActivo(true);
        documento.setTransportista(transportista);
        documento.setCreatedAt(LocalDateTime.now());

        // Configurar SecurityContext
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Debe guardar documento cuando transportista existe y no hay duplicado")
    void guardar_DebeGuardarDocumentoCorrectamente() {
        // Arrange
        Long transportistaId = 1L;

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.SOAT);
        doc.setFechaEmision(LocalDate.now());
        doc.setFechaVencimiento(LocalDate.of(2026, 12, 31));
        doc.setValor("Vigente");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(documentoRepository.existsByTransportistaIdAndTipoDocumentoAndDeletedAtIsNull(
                transportistaId, TipoDocumento.SOAT)).thenReturn(false);
        when(documentoRepository.save(any(DocumentoPersonal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        DocumentoPersonal resultado = documentoService.guardar(transportistaId, doc);

        // Assert
        assertNotNull(resultado);
        assertEquals(TipoDocumento.SOAT, resultado.getTipoDocumento());
        assertEquals(LocalDate.of(2026, 12, 31), resultado.getFechaVencimiento());
        assertEquals("Vigente", resultado.getValor());
        assertNotNull(resultado.getTransportista());
        assertEquals(transportistaId, resultado.getTransportista().getId());

        verify(transportistaRepository).findById(transportistaId);
        verify(documentoRepository).existsByTransportistaIdAndTipoDocumentoAndDeletedAtIsNull(
                transportistaId, TipoDocumento.SOAT);
        verify(documentoRepository).save(any(DocumentoPersonal.class));

        // Verificar auditoría
        ArgumentCaptor<AuditoriaDocumento> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaDocumento.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaDocumento auditoria = auditoriaCaptor.getValue();
        assertEquals("CREATE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
        assertEquals(transportistaId, auditoria.getTransportistaId());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el transportista no existe")
    void guardar_DebeLanzarExcepcionCuandoTransportistaNoExiste() {
        // Arrange
        Long transportistaId = 99L;

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.SOAT);
        doc.setFechaEmision(LocalDate.now());
        doc.setFechaVencimiento(LocalDate.of(2026, 12, 31));
        doc.setValor("Vigente");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> documentoService.guardar(transportistaId, doc));

        assertEquals("Transportista con ID 99 no existe", ex.getMessage());

        verify(documentoRepository, never()).existsByTransportistaIdAndTipoDocumentoAndDeletedAtIsNull(anyLong(), any());
        verify(documentoRepository, never()).save(any(DocumentoPersonal.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaDocumento.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el documento ya está registrado")
    void guardar_DebeLanzarExcepcionCuandoDocumentoYaExiste() {
        // Arrange
        Long transportistaId = 1L;

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.SOAT);
        doc.setFechaEmision(LocalDate.now());
        doc.setFechaVencimiento(LocalDate.of(2026, 12, 31));
        doc.setValor("Vigente");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(documentoRepository.existsByTransportistaIdAndTipoDocumentoAndDeletedAtIsNull(
                transportistaId, TipoDocumento.SOAT)).thenReturn(true);

        // Act & Assert
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> documentoService.guardar(transportistaId, doc));

        assertEquals("El documento tipo SOAT ya está registrado", ex.getMessage());

        verify(documentoRepository, never()).save(any(DocumentoPersonal.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaDocumento.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando SOAT no tiene fecha de vencimiento")
    void guardar_DebeLanzarExcepcionCuandoSoatNoTieneFechaVencimiento() {
        // Arrange
        Long transportistaId = 1L;

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.SOAT);
        doc.setFechaEmision(LocalDate.now());
        doc.setFechaVencimiento(null);
        doc.setValor("Vigente");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(documentoRepository.existsByTransportistaIdAndTipoDocumentoAndDeletedAtIsNull(
                transportistaId, TipoDocumento.SOAT)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> documentoService.guardar(transportistaId, doc));

        assertEquals("SOAT y REVISIÓN TÉCNICA requieren obligatoriamente fecha de emisión y vencimiento", ex.getMessage());

        verify(documentoRepository, never()).save(any(DocumentoPersonal.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaDocumento.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando SOAT tiene fecha de vencimiento anterior a emisión")
    void guardar_DebeLanzarExcepcionCuandoFechaVencimientoAnteriorAEmision() {
        // Arrange
        Long transportistaId = 1L;

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.SOAT);
        doc.setFechaEmision(LocalDate.of(2026, 12, 31));
        doc.setFechaVencimiento(LocalDate.of(2025, 1, 1)); // Vencimiento anterior a emisión
        doc.setValor("Vigente");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(documentoRepository.existsByTransportistaIdAndTipoDocumentoAndDeletedAtIsNull(
                transportistaId, TipoDocumento.SOAT)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> documentoService.guardar(transportistaId, doc));

        assertEquals("La fecha de vencimiento no puede ser anterior a la fecha de emisión", ex.getMessage());

        verify(documentoRepository, never()).save(any(DocumentoPersonal.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaDocumento.class));
    }

    @Test
    @DisplayName("Debe permitir guardar licencia con valor SI")
    void guardar_DebePermitirGuardarLicenciaConValorSi() {
        // Arrange
        Long transportistaId = 3L;

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.LICENCIA);
        doc.setValor("SI");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(documentoRepository.existsByTransportistaIdAndTipoDocumentoAndDeletedAtIsNull(
                transportistaId, TipoDocumento.LICENCIA)).thenReturn(false);
        when(documentoRepository.save(any(DocumentoPersonal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        DocumentoPersonal resultado = documentoService.guardar(transportistaId, doc);

        // Assert
        assertNotNull(resultado);
        assertEquals(TipoDocumento.LICENCIA, resultado.getTipoDocumento());
        assertEquals("SI", resultado.getValor());
        assertNull(resultado.getFechaEmision());
        assertNull(resultado.getFechaVencimiento());

        // Verificar auditoría
        ArgumentCaptor<AuditoriaDocumento> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaDocumento.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaDocumento auditoria = auditoriaCaptor.getValue();
        assertEquals("CREATE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando licencia tiene valor inválido")
    void guardar_DebeLanzarExcepcionCuandoLicenciaValorInvalido() {
        // Arrange
        Long transportistaId = 3L;

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.LICENCIA);
        doc.setValor("INVALIDO");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(documentoRepository.existsByTransportistaIdAndTipoDocumentoAndDeletedAtIsNull(
                transportistaId, TipoDocumento.LICENCIA)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> documentoService.guardar(transportistaId, doc));

        assertEquals("El valor para LICENCIA o TARJETA DE CIRCULACIÓN debe ser 'SI' o 'NO'", ex.getMessage());

        verify(documentoRepository, never()).save(any(DocumentoPersonal.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaDocumento.class));
    }

    @Test
    @DisplayName("Debe listar documentos por transportista")
    void listarPorTransportista_DebeRetornarDocumentos() {
        // Arrange
        Long transportistaId = 10L;
        when(documentoRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId))
                .thenReturn(List.of(documento, new DocumentoPersonal()));

        // Act
        List<DocumentoPersonal> resultado = documentoService.listarPorTransportista(transportistaId);

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals(TipoDocumento.SOAT, resultado.get(0).getTipoDocumento());

        verify(documentoRepository).findByTransportistaIdAndDeletedAtIsNull(transportistaId);
    }

    @Test
    @DisplayName("Debe obtener documento por ID")
    void obtenerPorId_DebeRetornarDocumento_CuandoExiste() {
        // Arrange
        Long documentoId = 1L;
        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(documento));

        // Act
        DocumentoPersonal resultado = documentoService.obtenerPorId(documentoId);

        // Assert
        assertNotNull(resultado);
        assertEquals(documentoId, resultado.getId());
        assertEquals(TipoDocumento.SOAT, resultado.getTipoDocumento());

        verify(documentoRepository).findById(documentoId);
    }

    @Test
    @DisplayName("Debe lanzar excepción al obtener documento por ID que no existe")
    void obtenerPorId_DebeLanzarExcepcion_CuandoNoExiste() {
        // Arrange
        Long documentoId = 999L;
        when(documentoRepository.findById(documentoId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> documentoService.obtenerPorId(documentoId));

        assertEquals("Documento con ID 999 no encontrado", ex.getMessage());
    }

    @Test
    @DisplayName("Debe actualizar documento correctamente")
    void actualizar_DebeActualizarDocumentoYAuditar() {
        // Arrange
        Long documentoId = 1L;

        DocumentoPersonal nuevosDatos = new DocumentoPersonal();
        nuevosDatos.setTipoDocumento(TipoDocumento.SOAT);
        nuevosDatos.setValor("Vencido");
        nuevosDatos.setFechaEmision(LocalDate.of(2025, 1, 1));
        nuevosDatos.setFechaVencimiento(LocalDate.of(2026, 12, 31));
        nuevosDatos.setActivo(true);

        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(documento));
        when(documentoRepository.save(any(DocumentoPersonal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        DocumentoPersonal resultado = documentoService.actualizar(documentoId, nuevosDatos);

        // Assert
        assertNotNull(resultado);
        assertEquals("Vencido", resultado.getValor());

        verify(documentoRepository).findById(documentoId);
        verify(documentoRepository).save(documento);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaDocumento> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaDocumento.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaDocumento auditoria = auditoriaCaptor.getValue();
        assertEquals("UPDATE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
        assertEquals("Vigente", auditoria.getValorAnterior());
        assertEquals("Vencido", auditoria.getValorNuevo());
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar documento que no existe")
    void actualizar_DebeLanzarExcepcion_CuandoDocumentoNoExiste() {
        // Arrange
        Long documentoId = 999L;
        DocumentoPersonal nuevosDatos = new DocumentoPersonal();

        when(documentoRepository.findById(documentoId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> documentoService.actualizar(documentoId, nuevosDatos));

        assertEquals("Documento con ID 999 no encontrado", ex.getMessage());

        verify(documentoRepository).findById(documentoId);
        verify(documentoRepository, never()).save(any());
        verify(auditoriaRepository, never()).save(any(AuditoriaDocumento.class));
    }

    @Test
    @DisplayName("Debe eliminar documento lógicamente (soft delete)")
    void eliminar_DebeEliminarDocumentoLogicamente() {
        // Arrange
        Long documentoId = 1L;
        String username = "admin";

        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(documento));
        when(documentoRepository.save(any(DocumentoPersonal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        documentoService.eliminar(documentoId, username);

        // Assert
        assertFalse(documento.getActivo());
        assertNotNull(documento.getDeletedAt());
        assertEquals(username, documento.getDeletedBy());

        verify(documentoRepository).findById(documentoId);
        verify(documentoRepository).save(documento);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaDocumento> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaDocumento.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaDocumento auditoria = auditoriaCaptor.getValue();
        assertEquals("DELETE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar documento que ya está eliminado")
    void eliminar_DebeLanzarExcepcion_CuandoDocumentoYaEliminado() {
        // Arrange
        Long documentoId = 1L;
        documento.setDeletedAt(LocalDateTime.now());
        documento.setDeletedBy("admin");

        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(documento));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> documentoService.eliminar(documentoId, "admin"));

        assertEquals("El documento ya está eliminado", ex.getMessage());
        verify(documentoRepository, never()).save(any(DocumentoPersonal.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar documento que no existe")
    void eliminar_DebeLanzarExcepcion_CuandoDocumentoNoExiste() {
        // Arrange
        Long documentoId = 999L;

        when(documentoRepository.findById(documentoId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> documentoService.eliminar(documentoId, "admin"));

        assertEquals("Documento con ID 999 no encontrado", ex.getMessage());
        verify(documentoRepository, never()).save(any(DocumentoPersonal.class));
    }

    @Test
    @DisplayName("Debe restaurar documento eliminado lógicamente")
    void restaurar_DebeRestaurarDocumentoEliminado() {
        // Arrange
        Long documentoId = 1L;
        documento.setDeletedAt(LocalDateTime.now());
        documento.setDeletedBy("admin");
        documento.setActivo(false);

        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(documento));
        when(documentoRepository.save(any(DocumentoPersonal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        documentoService.restaurar(documentoId);

        // Assert
        assertNull(documento.getDeletedAt());
        assertNull(documento.getDeletedBy());
        assertTrue(documento.getActivo());

        verify(documentoRepository).save(documento);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaDocumento> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaDocumento.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaDocumento auditoria = auditoriaCaptor.getValue();
        assertEquals("RESTORE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
    }

    @Test
    @DisplayName("Debe lanzar excepción al restaurar documento no eliminado")
    void restaurar_DebeLanzarExcepcion_CuandoDocumentoNoEliminado() {
        // Arrange
        Long documentoId = 1L;
        documento.setDeletedAt(null);

        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(documento));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> documentoService.restaurar(documentoId));

        assertEquals("El documento no está eliminado", ex.getMessage());
        verify(documentoRepository, never()).save(any(DocumentoPersonal.class));
    }

    @Test
    @DisplayName("Debe eliminar permanentemente un documento")
    void eliminarPermanente_DebeEliminarDocumentoFisicamente() {
        // Arrange
        Long documentoId = 1L;

        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(documento));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        documentoService.eliminarPermanente(documentoId);

        // Assert
        verify(documentoRepository).delete(documento);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaDocumento> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaDocumento.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaDocumento auditoria = auditoriaCaptor.getValue();
        assertEquals("DELETE_PERMANENT", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar permanentemente documento que no existe")
    void eliminarPermanente_DebeLanzarExcepcion_CuandoNoExiste() {
        // Arrange
        Long documentoId = 999L;

        when(documentoRepository.findById(documentoId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> documentoService.eliminarPermanente(documentoId));

        assertEquals("Documento con ID 999 no encontrado", ex.getMessage());
        verify(documentoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe cambiar estado del documento")
    void cambiarEstado_DebeCambiarEstadoDocumento() {
        // Arrange
        Long documentoId = 1L;
        Boolean nuevoEstado = false;

        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(documento));
        when(documentoRepository.save(any(DocumentoPersonal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        documentoService.cambiarEstado(documentoId, nuevoEstado);

        // Assert
        assertFalse(documento.getActivo());
        verify(documentoRepository).save(documento);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaDocumento> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaDocumento.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaDocumento auditoria = auditoriaCaptor.getValue();
        assertEquals("UPDATE_ESTADO", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
        assertTrue(auditoria.getActivoAnterior());
        assertFalse(auditoria.getActivoNuevo());
    }

    @Test
    @DisplayName("Debe listar documentos activos por transportista")
    void listarActivosPorTransportista_DebeRetornarSoloActivos() {
        // Arrange
        Long transportistaId = 1L;
        DocumentoPersonal docActivo = new DocumentoPersonal();
        docActivo.setTipoDocumento(TipoDocumento.SOAT);
        docActivo.setActivo(true);

        when(documentoRepository.findByTransportistaIdAndActivoTrueAndDeletedAtIsNull(transportistaId))
                .thenReturn(List.of(docActivo));

        // Act
        List<DocumentoPersonal> resultado = documentoService.listarActivosPorTransportista(transportistaId);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getActivo());

        verify(documentoRepository).findByTransportistaIdAndActivoTrueAndDeletedAtIsNull(transportistaId);
    }
}