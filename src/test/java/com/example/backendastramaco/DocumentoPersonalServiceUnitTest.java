package com.example.backendastramaco;

import com.example.backendastramaco.model.DocumentoPersonal;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.enums.TipoDocumento;
import com.example.backendastramaco.repository.DocumentoPersonalRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.service.DocumentoPersonalService;
import com.example.backendastramaco.service.audit.AuditService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentoPersonalServiceUnitTest {

    @Mock
    private DocumentoPersonalRepository repository;

    @Mock
    private TransportistaRepository transportistaRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private DocumentoPersonalService documentoPersonalService;

    @Test
    @DisplayName("Debe guardar documento cuando transportista existe y no hay duplicado")
    void guardar_DebeGuardarDocumentoCorrectamente() {
        Long transportistaId = 1L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.SOAT);
        doc.setFechaEmision(LocalDate.now()); // 🔥 Ajuste SonarQube: Requerido por la nueva validación
        doc.setFechaVencimiento(LocalDate.of(2026, 12, 31));
        doc.setValor("Vigente");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(repository.existsByTransportistaIdAndTipoDocumento(transportistaId, TipoDocumento.SOAT)).thenReturn(false);
        when(repository.save(any(DocumentoPersonal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(auditService).auditDocumento(any(), any(), anyString(), any(), any(), any());

        DocumentoPersonal resultado = documentoPersonalService.guardar(transportistaId, doc);

        assertNotNull(resultado);
        assertEquals(TipoDocumento.SOAT, resultado.getTipoDocumento());
        assertEquals(LocalDate.of(2026, 12, 31), resultado.getFechaVencimiento());
        assertEquals("Vigente", resultado.getValor());
        assertNotNull(resultado.getTransportista());
        assertEquals(transportistaId, resultado.getTransportista().getId());

        verify(transportistaRepository).findById(transportistaId);
        verify(repository).existsByTransportistaIdAndTipoDocumento(transportistaId, TipoDocumento.SOAT);
        verify(repository).save(any(DocumentoPersonal.class));
        verify(auditService, times(1)).auditDocumento(any(), eq(transportistaId), eq("CREATE"), isNull(), any(), any());
    }

    @Test
    @DisplayName("Debe guardar documento asignando transportista correctamente")
    void guardar_DebeAsignarTransportistaAlDocumento() {
        Long transportistaId = 2L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.REVISION_TECNICA);
        doc.setFechaEmision(LocalDate.now()); // 🔥 Ajuste SonarQube: Requerido por la nueva validación
        doc.setFechaVencimiento(LocalDate.of(2027, 1, 10));
        doc.setValor("Aprobado");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(repository.existsByTransportistaIdAndTipoDocumento(transportistaId, TipoDocumento.REVISION_TECNICA)).thenReturn(false);
        when(repository.save(any(DocumentoPersonal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(auditService).auditDocumento(any(), any(), anyString(), any(), any(), any());

        documentoPersonalService.guardar(transportistaId, doc);

        ArgumentCaptor<DocumentoPersonal> captor = ArgumentCaptor.forClass(DocumentoPersonal.class);
        verify(repository).save(captor.capture());

        DocumentoPersonal guardado = captor.getValue();

        assertNotNull(guardado.getTransportista());
        assertEquals(transportistaId, guardado.getTransportista().getId());
        assertEquals(TipoDocumento.REVISION_TECNICA, guardado.getTipoDocumento());

        verify(auditService, times(1)).auditDocumento(any(), eq(transportistaId), eq("CREATE"), isNull(), any(), any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el transportista no existe")
    void guardar_DebeLanzarExcepcionCuandoTransportistaNoExiste() {
        Long transportistaId = 99L;

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.SOAT);
        doc.setFechaEmision(LocalDate.now());
        doc.setFechaVencimiento(LocalDate.of(2026, 12, 31));
        doc.setValor("Vigente");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.empty());

        // 🔥 Ajuste: Cambiado RuntimeException por EntityNotFoundException
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> documentoPersonalService.guardar(transportistaId, doc));

        // 🔥 Ajuste: Mensaje sincronizado con la lógica mejorada del Service
        assertEquals("Transportista con ID 99 no existe", ex.getMessage());

        verify(repository, never()).existsByTransportistaIdAndTipoDocumento(anyLong(), any());
        verify(repository, never()).save(any(DocumentoPersonal.class));
        verify(auditService, never()).auditDocumento(any(), any(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el documento ya está registrado")
    void guardar_DebeLanzarExcepcionCuandoDocumentoYaExiste() {
        Long transportistaId = 1L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.SOAT);
        doc.setFechaEmision(LocalDate.now());
        doc.setFechaVencimiento(LocalDate.of(2026, 12, 31));
        doc.setValor("Vigente");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(repository.existsByTransportistaIdAndTipoDocumento(transportistaId, TipoDocumento.SOAT)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> documentoPersonalService.guardar(transportistaId, doc));

        // 🔥 Ajuste: Mensaje sincronizado dinámicamente con el enum del tipo de documento
        assertEquals("El documento tipo SOAT ya está registrado", ex.getMessage());

        verify(repository, never()).save(any(DocumentoPersonal.class));
        verify(auditService, never()).auditDocumento(any(), any(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando SOAT o REVISIÓN TÉCNICA no tienen fechas obligatorias")
    void guardar_DebeLanzarExcepcionCuandoSoatNoTieneFecha() {
        Long transportistaId = 1L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.SOAT);
        doc.setFechaEmision(LocalDate.now());
        doc.setFechaVencimiento(null); // 🔥 Forzamos la omisión de una de las fechas clave
        doc.setValor("Vigente");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(repository.existsByTransportistaIdAndTipoDocumento(transportistaId, TipoDocumento.SOAT)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> documentoPersonalService.guardar(transportistaId, doc));

        // 🔥 Ajuste: Mensaje de aserción corregido y mapeado con el Service
        assertEquals("SOAT y REVISIÓN TÉCNICA requieren obligatoriamente fecha de emisión y vencimiento", ex.getMessage());

        verify(repository, never()).save(any(DocumentoPersonal.class));
        verify(auditService, never()).auditDocumento(any(), any(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe permitir guardar licencia con valor SI")
    void guardar_DebePermitirGuardarLicenciaConValorSi() {
        Long transportistaId = 3L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.LICENCIA);
        doc.setValor("SI");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(repository.existsByTransportistaIdAndTipoDocumento(transportistaId, TipoDocumento.LICENCIA)).thenReturn(false);
        when(repository.save(any(DocumentoPersonal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(auditService).auditDocumento(any(), any(), anyString(), any(), any(), any());

        DocumentoPersonal resultado = documentoPersonalService.guardar(transportistaId, doc);

        assertNotNull(resultado);
        assertEquals(TipoDocumento.LICENCIA, resultado.getTipoDocumento());
        assertEquals("SI", resultado.getValor());

        verify(auditService, times(1)).auditDocumento(any(), eq(transportistaId), eq("CREATE"), isNull(), any(), any());
    }

    @Test
    @DisplayName("Debe listar documentos por transportista")
    void listarPorTransportista_DebeRetornarDocumentos() {
        Long transportistaId = 10L;

        DocumentoPersonal doc1 = new DocumentoPersonal();
        doc1.setTipoDocumento(TipoDocumento.SOAT);

        DocumentoPersonal doc2 = new DocumentoPersonal();
        doc2.setTipoDocumento(TipoDocumento.LICENCIA);

        when(repository.findByTransportistaId(transportistaId)).thenReturn(List.of(doc1, doc2));

        List<DocumentoPersonal> resultado = documentoPersonalService.listarPorTransportista(transportistaId);

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals(TipoDocumento.SOAT, resultado.get(0).getTipoDocumento());
        assertEquals(TipoDocumento.LICENCIA, resultado.get(1).getTipoDocumento());

        verify(repository).findByTransportistaId(transportistaId);
    }
}