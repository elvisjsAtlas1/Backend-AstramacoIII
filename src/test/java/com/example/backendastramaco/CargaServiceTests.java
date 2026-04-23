package com.example.backendastramaco;

import com.example.backendastramaco.dto.AumentarCargaRequestDTO;
import com.example.backendastramaco.dto.CargaRequestDTO;
import com.example.backendastramaco.dto.CargaResponseDTO;
import com.example.backendastramaco.model.Carga;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.enums.TipoMaterial;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.CargaRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.service.CargaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CargaServiceTests {

    @Mock
    private CargaRepository cargaRepository;

    @Mock
    private TransportistaRepository transportistaRepository;

    @InjectMocks
    private CargaService cargaService;

    @Test
    @DisplayName("Debe subir carga nueva para transportista camionero")
    void subirCargaActual_DebeCrearNuevaCarga() {
        Long transportistaId = 1L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);
        transportista.setNombre("Juan");
        transportista.setApellidos("Perez");
        transportista.setTipoTransporte(TipoTransporte.CAMIONERO);

        CargaRequestDTO request = new CargaRequestDTO();
        request.setTipoMaterial(TipoMaterial.PANDERETA);
        request.setCantidadDisponible(80.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaId(transportistaId)).thenReturn(Optional.empty());
        when(cargaRepository.save(any(Carga.class))).thenAnswer(invocation -> {
            Carga carga = invocation.getArgument(0);
            carga.setId(10L);
            return carga;
        });

        CargaResponseDTO resultado = cargaService.subirCargaActual(transportistaId, request);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals(transportistaId, resultado.getTransportistaId());
        assertEquals("Juan Perez", resultado.getTransportistaNombre());
        assertEquals(TipoMaterial.PANDERETA, resultado.getTipoMaterial());
        assertEquals(80.0, resultado.getCantidadDisponible());
    }

    @Test
    @DisplayName("Debe reemplazar datos de carga existente al subir carga actual")
    void subirCargaActual_DebeActualizarCargaExistente() {
        Long transportistaId = 2L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);
        transportista.setNombre("Luis");
        transportista.setApellidos("Mamani");
        transportista.setTipoTransporte(TipoTransporte.CAMIONERO);

        Carga cargaExistente = new Carga();
        cargaExistente.setId(20L);
        cargaExistente.setTransportista(transportista);
        cargaExistente.setTipoMaterial(TipoMaterial.TECHO);
        cargaExistente.setCantidadDisponible(40.0);

        CargaRequestDTO request = new CargaRequestDTO();
        request.setTipoMaterial(TipoMaterial.PANDERETA);
        request.setCantidadDisponible(100.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaId(transportistaId)).thenReturn(Optional.of(cargaExistente));
        when(cargaRepository.save(any(Carga.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CargaResponseDTO resultado = cargaService.subirCargaActual(transportistaId, request);

        assertNotNull(resultado);
        assertEquals(20L, resultado.getId());
        assertEquals(TipoMaterial.PANDERETA, resultado.getTipoMaterial());
        assertEquals(100.0, resultado.getCantidadDisponible());
        assertEquals("Luis Mamani", resultado.getTransportistaNombre());
    }

    @Test
    @DisplayName("Debe lanzar excepción al subir carga si transportista no existe")
    void subirCargaActual_DebeLanzarExcepcionCuandoTransportistaNoExiste() {
        Long transportistaId = 99L;

        CargaRequestDTO request = new CargaRequestDTO();
        request.setTipoMaterial(TipoMaterial.PANDERETA);
        request.setCantidadDisponible(50.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cargaService.subirCargaActual(transportistaId, request));

        assertEquals("Transportista no existe", ex.getMessage());

        verify(cargaRepository, never()).save(any(Carga.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al subir carga si transportista no es camionero")
    void subirCargaActual_DebeLanzarExcepcionCuandoNoEsCamionero() {
        Long transportistaId = 3L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);
        transportista.setTipoTransporte(TipoTransporte.VOLQUETERO);

        CargaRequestDTO request = new CargaRequestDTO();
        request.setTipoMaterial(TipoMaterial.PANDERETA);
        request.setCantidadDisponible(50.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cargaService.subirCargaActual(transportistaId, request));

        assertEquals("Solo los transportistas CAMIONERO pueden manejar carga", ex.getMessage());

        verify(cargaRepository, never()).save(any(Carga.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al subir carga con material inválido")
    void subirCargaActual_DebeLanzarExcepcionCuandoMaterialEsInvalido() {
        Long transportistaId = 4L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);
        transportista.setTipoTransporte(TipoTransporte.CAMIONERO);

        CargaRequestDTO request = new CargaRequestDTO();
        request.setTipoMaterial(TipoMaterial.PIEDRA);
        request.setCantidadDisponible(30.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cargaService.subirCargaActual(transportistaId, request));

        assertEquals("El transportista CAMIONERO solo puede registrar PANDERETA o TECHO", ex.getMessage());

        verify(cargaRepository, never()).save(any(Carga.class));
    }

    @Test
    @DisplayName("Debe aumentar carga existente cuando el material coincide")
    void aumentarCargaActual_DebeAumentarCantidad() {
        Long transportistaId = 5L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);
        transportista.setNombre("Mario");
        transportista.setApellidos("Quispe");
        transportista.setTipoTransporte(TipoTransporte.CAMIONERO);

        Carga carga = new Carga();
        carga.setId(50L);
        carga.setTransportista(transportista);
        carga.setTipoMaterial(TipoMaterial.PANDERETA);
        carga.setCantidadDisponible(70.0);

        AumentarCargaRequestDTO request = new AumentarCargaRequestDTO();
        request.setTipoMaterial(TipoMaterial.PANDERETA);
        request.setCantidadAgregar(25.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaId(transportistaId)).thenReturn(Optional.of(carga));
        when(cargaRepository.save(any(Carga.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CargaResponseDTO resultado = cargaService.aumentarCargaActual(transportistaId, request);

        assertNotNull(resultado);
        assertEquals(95.0, resultado.getCantidadDisponible());
        assertEquals(TipoMaterial.PANDERETA, resultado.getTipoMaterial());
        assertEquals("Mario Quispe", resultado.getTransportistaNombre());
    }

    @Test
    @DisplayName("Debe lanzar excepción al aumentar carga si no existe carga registrada")
    void aumentarCargaActual_DebeLanzarExcepcionCuandoNoExisteCarga() {
        Long transportistaId = 6L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);
        transportista.setTipoTransporte(TipoTransporte.CAMIONERO);

        AumentarCargaRequestDTO request = new AumentarCargaRequestDTO();
        request.setTipoMaterial(TipoMaterial.PANDERETA);
        request.setCantidadAgregar(20.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaId(transportistaId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cargaService.aumentarCargaActual(transportistaId, request));

        assertEquals("El transportista no tiene carga registrada", ex.getMessage());

        verify(cargaRepository, never()).save(any(Carga.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al aumentar carga si el material es distinto")
    void aumentarCargaActual_DebeLanzarExcepcionCuandoMaterialEsDistinto() {
        Long transportistaId = 7L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);
        transportista.setTipoTransporte(TipoTransporte.CAMIONERO);

        Carga carga = new Carga();
        carga.setTransportista(transportista);
        carga.setTipoMaterial(TipoMaterial.TECHO);
        carga.setCantidadDisponible(60.0);

        AumentarCargaRequestDTO request = new AumentarCargaRequestDTO();
        request.setTipoMaterial(TipoMaterial.PANDERETA);
        request.setCantidadAgregar(10.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaId(transportistaId)).thenReturn(Optional.of(carga));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cargaService.aumentarCargaActual(transportistaId, request));

        assertEquals("Solo se puede aumentar si el material es el mismo que la carga actual", ex.getMessage());

        verify(cargaRepository, never()).save(any(Carga.class));
    }

    @Test
    @DisplayName("Debe obtener carga registrada del transportista")
    void obtenerCarga_DebeRetornarCarga() {
        Long transportistaId = 8L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);
        transportista.setNombre("Joel");
        transportista.setApellidos("Flores");
        transportista.setTipoTransporte(TipoTransporte.CAMIONERO);

        Carga carga = new Carga();
        carga.setId(80L);
        carga.setTransportista(transportista);
        carga.setTipoMaterial(TipoMaterial.TECHO);
        carga.setCantidadDisponible(45.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaId(transportistaId)).thenReturn(Optional.of(carga));

        CargaResponseDTO resultado = cargaService.obtenerCarga(transportistaId);

        assertNotNull(resultado);
        assertEquals(80L, resultado.getId());
        assertEquals(transportistaId, resultado.getTransportistaId());
        assertEquals("Joel Flores", resultado.getTransportistaNombre());
        assertEquals(TipoMaterial.TECHO, resultado.getTipoMaterial());
        assertEquals(45.0, resultado.getCantidadDisponible());
    }

    @Test
    @DisplayName("Debe lanzar excepción al obtener carga si aún no existe")
    void obtenerCarga_DebeLanzarExcepcionCuandoNoHayCargaRegistrada() {
        Long transportistaId = 9L;

        Transportista transportista = new Transportista();
        transportista.setId(transportistaId);
        transportista.setTipoTransporte(TipoTransporte.CAMIONERO);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaId(transportistaId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cargaService.obtenerCarga(transportistaId));

        assertEquals("El transportista aún no tiene carga registrada", ex.getMessage());
    }

    @Test
    @DisplayName("Debe listar todas las cargas")
    void listarTodas_DebeRetornarListaDeCargas() {
        Transportista t1 = new Transportista();
        t1.setId(1L);
        t1.setNombre("Juan");
        t1.setApellidos("Perez");

        Transportista t2 = new Transportista();
        t2.setId(2L);
        t2.setNombre("Luis");
        t2.setApellidos("Mamani");

        Carga c1 = new Carga();
        c1.setId(101L);
        c1.setTransportista(t1);
        c1.setTipoMaterial(TipoMaterial.PANDERETA);
        c1.setCantidadDisponible(30.0);

        Carga c2 = new Carga();
        c2.setId(102L);
        c2.setTransportista(t2);
        c2.setTipoMaterial(TipoMaterial.TECHO);
        c2.setCantidadDisponible(55.0);

        when(cargaRepository.findAll()).thenReturn(List.of(c1, c2));

        List<CargaResponseDTO> resultado = cargaService.listarTodas();

        assertNotNull(resultado);
        assertEquals(2, resultado.size());

        assertEquals(101L, resultado.get(0).getId());
        assertEquals("Juan Perez", resultado.get(0).getTransportistaNombre());
        assertEquals(TipoMaterial.PANDERETA, resultado.get(0).getTipoMaterial());

        assertEquals(102L, resultado.get(1).getId());
        assertEquals("Luis Mamani", resultado.get(1).getTransportistaNombre());
        assertEquals(TipoMaterial.TECHO, resultado.get(1).getTipoMaterial());
    }
}