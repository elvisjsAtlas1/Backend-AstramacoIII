package com.example.backendastramaco;

import com.example.backendastramaco.model.DocumentoPersonal;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.enums.TipoDocumento;
import com.example.backendastramaco.repository.DocumentoPersonalRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.service.DocumentoPersonalService;
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
class DocumentoPersonalServiceTests {

    @Mock
    private DocumentoPersonalRepository repository;

    @Mock
    private TransportistaRepository transportistaRepository;

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
        doc.setFechaVencimiento(LocalDate.of(2026, 12, 31));
        doc.setValor("Vigente");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(repository.existsByTransportistaIdAndTipoDocumento(transportistaId, TipoDocumento.SOAT)).thenReturn(false);
        when(repository.save(any(DocumentoPersonal.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
    }

    @Test
    @DisplayName("Debe guardar documento asignando transportista correctamente")
    void guardar_DebeAsignarTransportistaAlDocumento() {
        Long transportistaId = 2L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.REVISION_TECNICA);
        doc.setFechaVencimiento(LocalDate.of(2027, 1, 10));
        doc.setValor("Aprobado");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(repository.existsByTransportistaIdAndTipoDocumento(transportistaId, TipoDocumento.REVISION_TECNICA)).thenReturn(false);
        when(repository.save(any(DocumentoPersonal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        documentoPersonalService.guardar(transportistaId, doc);

        ArgumentCaptor<DocumentoPersonal> captor = ArgumentCaptor.forClass(DocumentoPersonal.class);
        verify(repository).save(captor.capture());

        DocumentoPersonal guardado = captor.getValue();

        assertNotNull(guardado.getTransportista());
        assertEquals(transportistaId, guardado.getTransportista().getId());
        assertEquals(TipoDocumento.REVISION_TECNICA, guardado.getTipoDocumento());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el transportista no existe")
    void guardar_DebeLanzarExcepcionCuandoTransportistaNoExiste() {
        Long transportistaId = 99L;

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.SOAT);
        doc.setFechaVencimiento(LocalDate.of(2026, 12, 31));
        doc.setValor("Vigente");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> documentoPersonalService.guardar(transportistaId, doc));

        assertEquals("Transportista no existe", ex.getMessage());

        verify(repository, never()).existsByTransportistaIdAndTipoDocumento(anyLong(), any());
        verify(repository, never()).save(any(DocumentoPersonal.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el documento ya está registrado")
    void guardar_DebeLanzarExcepcionCuandoDocumentoYaExiste() {
        Long transportistaId = 1L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.SOAT);
        doc.setFechaVencimiento(LocalDate.of(2026, 12, 31));
        doc.setValor("Vigente");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(repository.existsByTransportistaIdAndTipoDocumento(transportistaId, TipoDocumento.SOAT)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> documentoPersonalService.guardar(transportistaId, doc));

        assertEquals("Documento ya registrado", ex.getMessage());

        verify(repository, never()).save(any(DocumentoPersonal.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando SOAT no tiene fecha de vencimiento")
    void guardar_DebeLanzarExcepcionCuandoSoatNoTieneFecha() {
        Long transportistaId = 1L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.SOAT);
        doc.setFechaVencimiento(null);
        doc.setValor("Vigente");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(repository.existsByTransportistaIdAndTipoDocumento(transportistaId, TipoDocumento.SOAT)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> documentoPersonalService.guardar(transportistaId, doc));

        assertEquals("Requiere fecha", ex.getMessage());

        verify(repository, never()).save(any(DocumentoPersonal.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando revisión técnica no tiene fecha de vencimiento")
    void guardar_DebeLanzarExcepcionCuandoRevisionTecnicaNoTieneFecha() {
        Long transportistaId = 1L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.REVISION_TECNICA);
        doc.setFechaVencimiento(null);
        doc.setValor("Aprobado");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(repository.existsByTransportistaIdAndTipoDocumento(transportistaId, TipoDocumento.REVISION_TECNICA)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> documentoPersonalService.guardar(transportistaId, doc));

        assertEquals("Requiere fecha", ex.getMessage());

        verify(repository, never()).save(any(DocumentoPersonal.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando licencia no tiene valor SI o NO")
    void guardar_DebeLanzarExcepcionCuandoLicenciaTieneValorInvalido() {
        Long transportistaId = 1L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.LICENCIA);
        doc.setValor("TAL VEZ");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(repository.existsByTransportistaIdAndTipoDocumento(transportistaId, TipoDocumento.LICENCIA)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> documentoPersonalService.guardar(transportistaId, doc));

        assertEquals("Debe ser SI o NO", ex.getMessage());

        verify(repository, never()).save(any(DocumentoPersonal.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando tarjeta de circulación no tiene valor SI o NO")
    void guardar_DebeLanzarExcepcionCuandoTarjetaCirculacionTieneValorInvalido() {
        Long transportistaId = 1L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);

        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.TARJETA_CIRCULACION);
        doc.setValor("QUIZÁ");

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(repository.existsByTransportistaIdAndTipoDocumento(transportistaId, TipoDocumento.TARJETA_CIRCULACION)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> documentoPersonalService.guardar(transportistaId, doc));

        assertEquals("Debe ser SI o NO", ex.getMessage());

        verify(repository, never()).save(any(DocumentoPersonal.class));
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

        DocumentoPersonal resultado = documentoPersonalService.guardar(transportistaId, doc);

        assertNotNull(resultado);
        assertEquals(TipoDocumento.LICENCIA, resultado.getTipoDocumento());
        assertEquals("SI", resultado.getValor());
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