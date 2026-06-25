package com.example.backendastramaco.unitarias;

import com.example.backendastramaco.dto.AumentarCargaRequestDTO;
import com.example.backendastramaco.dto.CargaRequestDTO;
import com.example.backendastramaco.dto.CargaResponseDTO;
import com.example.backendastramaco.exception.DuplicateResourceException;
import com.example.backendastramaco.exception.ResourceNotFoundException;
import com.example.backendastramaco.model.Carga;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.audit.AuditoriaCarga;
import com.example.backendastramaco.model.enums.TipoMaterial;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.CargaRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.audit.AuditoriaCargaRepository;
import com.example.backendastramaco.service.CargaService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CargaServiceUnitTest {

    @Mock
    private CargaRepository cargaRepository;

    @Mock
    private TransportistaRepository transportistaRepository;

    @Mock
    private AuditoriaCargaRepository auditoriaRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private CargaService cargaService;

    private Carga carga;
    private Transportista transportista;

    @BeforeEach
    void setUp() {
        // Configurar transportista base
        transportista = new Transportista();
        ReflectionTestUtils.setField(transportista, "id", 1L);
        transportista.setNombre("Juan");
        transportista.setApellidos("Perez");
        transportista.setTipoTransporte(TipoTransporte.CAMIONERO);

        // Configurar carga base
        carga = new Carga();
        ReflectionTestUtils.setField(carga, "id", 10L);
        carga.setTransportista(transportista);
        carga.setTipoMaterial(TipoMaterial.PANDERETA);
        carga.setCantidadDisponible(80.0);
        carga.setCreatedAt(LocalDateTime.now());

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
    @DisplayName("Debe crear carga nueva para transportista camionero")
    void crearCarga_DebeCrearNuevaCarga() {
        // Arrange
        Long transportistaId = 1L;

        CargaRequestDTO requestDTO = new CargaRequestDTO();
        requestDTO.setTipoMaterial(TipoMaterial.PANDERETA);
        requestDTO.setCantidadDisponible(80.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.existsByTransportistaIdAndDeletedAtIsNull(transportistaId)).thenReturn(false);
        when(cargaRepository.save(any(Carga.class))).thenAnswer(invocation -> {
            Carga c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 10L);
            return c;
        });
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        CargaResponseDTO resultado = cargaService.crearCarga(transportistaId, requestDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals(transportistaId, resultado.getTransportistaId());
        assertEquals("Juan Perez", resultado.getTransportistaNombre());
        assertEquals(TipoMaterial.PANDERETA, resultado.getTipoMaterial());
        assertEquals(80.0, resultado.getCantidadDisponible());

        verify(cargaRepository).existsByTransportistaIdAndDeletedAtIsNull(transportistaId);
        verify(cargaRepository).save(any(Carga.class));

        // Verificar auditoría
        ArgumentCaptor<AuditoriaCarga> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaCarga.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaCarga auditoria = auditoriaCaptor.getValue();
        assertEquals("CREATE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
        assertEquals(transportistaId, auditoria.getTransportistaId());
    }

    @Test
    @DisplayName("Debe lanzar excepción al crear carga si ya existe")
    void crearCarga_DebeLanzarExcepcionCuandoYaExisteCarga() {
        // Arrange
        Long transportistaId = 1L;

        CargaRequestDTO requestDTO = new CargaRequestDTO();
        requestDTO.setTipoMaterial(TipoMaterial.PANDERETA);
        requestDTO.setCantidadDisponible(80.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.existsByTransportistaIdAndDeletedAtIsNull(transportistaId)).thenReturn(true);

        // Act & Assert
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> cargaService.crearCarga(transportistaId, requestDTO));

        assertEquals("El transportista ya tiene una carga registrada", ex.getMessage());

        verify(cargaRepository, never()).save(any(Carga.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaCarga.class));
    }

    @Test
    @DisplayName("Debe actualizar carga existente")
    void actualizarCarga_DebeActualizarCargaExistente() {
        // Arrange
        Long transportistaId = 1L;

        CargaRequestDTO requestDTO = new CargaRequestDTO();
        requestDTO.setTipoMaterial(TipoMaterial.TECHO);
        requestDTO.setCantidadDisponible(100.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId)).thenReturn(Optional.of(carga));
        when(cargaRepository.save(any(Carga.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        CargaResponseDTO resultado = cargaService.actualizarCarga(transportistaId, requestDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals(TipoMaterial.TECHO, resultado.getTipoMaterial());
        assertEquals(100.0, resultado.getCantidadDisponible());
        assertEquals("Juan Perez", resultado.getTransportistaNombre());

        verify(cargaRepository).findByTransportistaIdAndDeletedAtIsNull(transportistaId);
        verify(cargaRepository).save(any(Carga.class));

        // Verificar auditoría
        ArgumentCaptor<AuditoriaCarga> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaCarga.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaCarga auditoria = auditoriaCaptor.getValue();
        assertEquals("UPDATE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
        assertEquals("PANDERETA", auditoria.getTipoMaterialAnterior());
        assertEquals("TECHO", auditoria.getTipoMaterialNuevo());
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar carga si no existe")
    void actualizarCarga_DebeLanzarExcepcionCuandoNoExisteCarga() {
        // Arrange
        Long transportistaId = 1L;

        CargaRequestDTO requestDTO = new CargaRequestDTO();
        requestDTO.setTipoMaterial(TipoMaterial.PANDERETA);
        requestDTO.setCantidadDisponible(80.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> cargaService.actualizarCarga(transportistaId, requestDTO));

        assertEquals("El transportista no tiene carga registrada", ex.getMessage());

        verify(cargaRepository, never()).save(any(Carga.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaCarga.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al subir carga si transportista no existe")
    void subirCargaActual_DebeLanzarExcepcionCuandoTransportistaNoExiste() {
        // Arrange
        Long transportistaId = 99L;

        CargaRequestDTO requestDTO = new CargaRequestDTO();
        requestDTO.setTipoMaterial(TipoMaterial.PANDERETA);
        requestDTO.setCantidadDisponible(50.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> cargaService.crearCarga(transportistaId, requestDTO));

        assertEquals("Transportista con ID 99 no existe", ex.getMessage());

        verify(cargaRepository, never()).save(any(Carga.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaCarga.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al subir carga si transportista no es camionero")
    void subirCargaActual_DebeLanzarExcepcionCuandoNoEsCamionero() {
        // Arrange
        Long transportistaId = 3L;

        Transportista transportistaVolquetero = new Transportista();
        ReflectionTestUtils.setField(transportistaVolquetero, "id", 3L);
        transportistaVolquetero.setTipoTransporte(TipoTransporte.VOLQUETERO);

        CargaRequestDTO requestDTO = new CargaRequestDTO();
        requestDTO.setTipoMaterial(TipoMaterial.PANDERETA);
        requestDTO.setCantidadDisponible(50.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportistaVolquetero));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cargaService.crearCarga(transportistaId, requestDTO));

        assertEquals("Solo los transportistas CAMIONERO pueden manejar carga", ex.getMessage());

        verify(cargaRepository, never()).save(any(Carga.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaCarga.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al subir carga con material inválido")
    void subirCargaActual_DebeLanzarExcepcionCuandoMaterialEsInvalido() {
        // Arrange
        Long transportistaId = 4L;

        Transportista transportistaCamionero = new Transportista();
        ReflectionTestUtils.setField(transportistaCamionero, "id", 4L);
        transportistaCamionero.setTipoTransporte(TipoTransporte.CAMIONERO);

        CargaRequestDTO requestDTO = new CargaRequestDTO();
        requestDTO.setTipoMaterial(TipoMaterial.PIEDRA);
        requestDTO.setCantidadDisponible(30.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportistaCamionero));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cargaService.crearCarga(transportistaId, requestDTO));

        assertEquals("El transportista CAMIONERO solo puede registrar PANDERETA o TECHO", ex.getMessage());

        verify(cargaRepository, never()).save(any(Carga.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaCarga.class));
    }

    @Test
    @DisplayName("Debe aumentar carga existente cuando el material coincide")
    void aumentarCarga_DebeAumentarCantidad() {
        // Arrange
        Long transportistaId = 1L;

        AumentarCargaRequestDTO requestDTO = new AumentarCargaRequestDTO();
        requestDTO.setTipoMaterial(TipoMaterial.PANDERETA);
        requestDTO.setCantidadAgregar(25.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId)).thenReturn(Optional.of(carga));
        when(cargaRepository.save(any(Carga.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        CargaResponseDTO resultado = cargaService.aumentarCarga(transportistaId, requestDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals(105.0, resultado.getCantidadDisponible());
        assertEquals(TipoMaterial.PANDERETA, resultado.getTipoMaterial());
        assertEquals("Juan Perez", resultado.getTransportistaNombre());

        verify(cargaRepository).findByTransportistaIdAndDeletedAtIsNull(transportistaId);
        verify(cargaRepository).save(any(Carga.class));

        // Verificar auditoría
        ArgumentCaptor<AuditoriaCarga> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaCarga.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaCarga auditoria = auditoriaCaptor.getValue();
        assertEquals("UPDATE_AUMENTO", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
        assertEquals(80.0, auditoria.getCantidadAnterior());
        assertEquals(105.0, auditoria.getCantidadNuevo());
    }

    @Test
    @DisplayName("Debe lanzar excepción al aumentar carga si no existe carga registrada")
    void aumentarCarga_DebeLanzarExcepcionCuandoNoExisteCarga() {
        // Arrange
        Long transportistaId = 6L;

        AumentarCargaRequestDTO requestDTO = new AumentarCargaRequestDTO();
        requestDTO.setTipoMaterial(TipoMaterial.PANDERETA);
        requestDTO.setCantidadAgregar(20.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> cargaService.aumentarCarga(transportistaId, requestDTO));

        assertEquals("El transportista no tiene carga registrada", ex.getMessage());

        verify(cargaRepository, never()).save(any(Carga.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaCarga.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al aumentar carga si el material es distinto")
    void aumentarCarga_DebeLanzarExcepcionCuandoMaterialEsDistinto() {
        // Arrange
        Long transportistaId = 7L;

        carga.setTipoMaterial(TipoMaterial.TECHO);

        AumentarCargaRequestDTO requestDTO = new AumentarCargaRequestDTO();
        requestDTO.setTipoMaterial(TipoMaterial.PANDERETA);
        requestDTO.setCantidadAgregar(10.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId)).thenReturn(Optional.of(carga));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cargaService.aumentarCarga(transportistaId, requestDTO));

        assertEquals("Solo se puede aumentar si el material es el mismo que la carga actual", ex.getMessage());

        verify(cargaRepository, never()).save(any(Carga.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaCarga.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al aumentar carga con cantidad negativa o cero")
    void aumentarCarga_DebeLanzarExcepcionCuandoCantidadInvalida() {
        // Arrange
        Long transportistaId = 1L;

        AumentarCargaRequestDTO requestDTO = new AumentarCargaRequestDTO();
        requestDTO.setTipoMaterial(TipoMaterial.PANDERETA);
        requestDTO.setCantidadAgregar(-5.0);

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId)).thenReturn(Optional.of(carga));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cargaService.aumentarCarga(transportistaId, requestDTO));

        assertEquals("La cantidad a agregar debe ser mayor a cero", ex.getMessage());

        verify(cargaRepository, never()).save(any(Carga.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaCarga.class));
    }

    @Test
    @DisplayName("Debe obtener carga registrada del transportista")
    void obtenerCarga_DebeRetornarCarga() {
        // Arrange
        Long transportistaId = 1L;

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId)).thenReturn(Optional.of(carga));

        // Act
        CargaResponseDTO resultado = cargaService.obtenerCarga(transportistaId);

        // Assert
        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals(transportistaId, resultado.getTransportistaId());
        assertEquals("Juan Perez", resultado.getTransportistaNombre());
        assertEquals(TipoMaterial.PANDERETA, resultado.getTipoMaterial());
        assertEquals(80.0, resultado.getCantidadDisponible());
    }

    @Test
    @DisplayName("Debe lanzar excepción al obtener carga si aún no existe")
    void obtenerCarga_DebeLanzarExcepcionCuandoNoHayCargaRegistrada() {
        // Arrange
        Long transportistaId = 9L;

        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> cargaService.obtenerCarga(transportistaId));

        assertEquals("El transportista aún no tiene carga registrada", ex.getMessage());
    }

    @Test
    @DisplayName("Debe obtener carga por ID")
    void obtenerPorId_DebeRetornarCarga() {
        // Arrange
        Long cargaId = 10L;
        when(cargaRepository.findByIdAndDeletedAtIsNull(cargaId)).thenReturn(Optional.of(carga));

        // Act
        CargaResponseDTO resultado = cargaService.obtenerPorId(cargaId);

        // Assert
        assertNotNull(resultado);
        assertEquals(cargaId, resultado.getId());
        assertEquals(TipoMaterial.PANDERETA, resultado.getTipoMaterial());
        assertEquals(80.0, resultado.getCantidadDisponible());

        verify(cargaRepository).findByIdAndDeletedAtIsNull(cargaId);
    }

    @Test
    @DisplayName("Debe lanzar excepción al obtener carga por ID que no existe")
    void obtenerPorId_DebeLanzarExcepcion_CuandoNoExiste() {
        // Arrange
        Long cargaId = 999L;
        when(cargaRepository.findByIdAndDeletedAtIsNull(cargaId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> cargaService.obtenerPorId(cargaId));

        assertEquals("Carga no encontrada con ID: 999", ex.getMessage());
    }

    @Test
    @DisplayName("Debe listar todas las cargas con paginación")
    void listarTodas_DebeRetornarPaginaDeCargas() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Carga> cargaPage = new PageImpl<>(List.of(carga));
        when(cargaRepository.findByDeletedAtIsNull(pageable)).thenReturn(cargaPage);

        // Act
        Page<CargaResponseDTO> resultado = cargaService.listarTodas(pageable, null);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertEquals("Juan Perez", resultado.getContent().get(0).getTransportistaNombre());

        verify(cargaRepository).findByDeletedAtIsNull(pageable);
    }

    @Test
    @DisplayName("Debe listar cargas con filtro por tipo de material")
    void listarTodas_DebeFiltrarPorTipoMaterial() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Carga> cargaPage = new PageImpl<>(List.of(carga));
        when(cargaRepository.findByTipoMaterialAndDeletedAtIsNull(TipoMaterial.PANDERETA, pageable))
                .thenReturn(cargaPage);

        // Act
        Page<CargaResponseDTO> resultado = cargaService.listarTodas(pageable, "PANDERETA");

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertEquals(TipoMaterial.PANDERETA, resultado.getContent().get(0).getTipoMaterial());

        verify(cargaRepository).findByTipoMaterialAndDeletedAtIsNull(TipoMaterial.PANDERETA, pageable);
    }

    @Test
    @DisplayName("Debe eliminar carga lógicamente (soft delete)")
    void eliminar_DebeEliminarCargaLogicamente() {
        // Arrange
        Long cargaId = 10L;
        String username = "admin";

        when(cargaRepository.findByIdAndDeletedAtIsNull(cargaId)).thenReturn(Optional.of(carga));
        when(cargaRepository.save(any(Carga.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        cargaService.eliminar(cargaId, username);

        // Assert
        assertNotNull(carga.getDeletedAt());
        assertEquals(username, carga.getDeletedBy());

        verify(cargaRepository).findByIdAndDeletedAtIsNull(cargaId);
        verify(cargaRepository).save(carga);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaCarga> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaCarga.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaCarga auditoria = auditoriaCaptor.getValue();
        assertEquals("DELETE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar carga que ya está eliminada")
    void eliminar_DebeLanzarExcepcion_CuandoCargaYaEliminada() {
        // Arrange
        Long cargaId = 10L;
        carga.setDeletedAt(LocalDateTime.now());
        carga.setDeletedBy("admin");

        when(cargaRepository.findByIdAndDeletedAtIsNull(cargaId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> cargaService.eliminar(cargaId, "admin"));

        assertEquals("Carga no encontrada con ID: 10", ex.getMessage());
        verify(cargaRepository, never()).save(any(Carga.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar carga que no existe")
    void eliminar_DebeLanzarExcepcion_CuandoCargaNoExiste() {
        // Arrange
        Long cargaId = 999L;

        when(cargaRepository.findByIdAndDeletedAtIsNull(cargaId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> cargaService.eliminar(cargaId, "admin"));

        assertEquals("Carga no encontrada con ID: 999", ex.getMessage());
        verify(cargaRepository, never()).save(any(Carga.class));
    }

    @Test
    @DisplayName("Debe restaurar carga eliminada lógicamente")
    void restaurar_DebeRestaurarCargaEliminada() {
        // Arrange
        Long cargaId = 10L;
        carga.setDeletedAt(LocalDateTime.now());
        carga.setDeletedBy("admin");

        when(cargaRepository.findById(cargaId)).thenReturn(Optional.of(carga));
        when(cargaRepository.save(any(Carga.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        cargaService.restaurar(cargaId);

        // Assert
        assertNull(carga.getDeletedAt());
        assertNull(carga.getDeletedBy());

        verify(cargaRepository).save(carga);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaCarga> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaCarga.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaCarga auditoria = auditoriaCaptor.getValue();
        assertEquals("RESTORE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
    }

    @Test
    @DisplayName("Debe lanzar excepción al restaurar carga no eliminada")
    void restaurar_DebeLanzarExcepcion_CuandoCargaNoEliminada() {
        // Arrange
        Long cargaId = 10L;
        carga.setDeletedAt(null);

        when(cargaRepository.findById(cargaId)).thenReturn(Optional.of(carga));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cargaService.restaurar(cargaId));

        assertEquals("La carga no está eliminada", ex.getMessage());
        verify(cargaRepository, never()).save(any(Carga.class));
    }

    @Test
    @DisplayName("Debe eliminar permanentemente una carga")
    void eliminarPermanente_DebeEliminarCargaFisicamente() {
        // Arrange
        Long cargaId = 10L;

        when(cargaRepository.findById(cargaId)).thenReturn(Optional.of(carga));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        cargaService.eliminarPermanente(cargaId);

        // Assert
        verify(cargaRepository).delete(carga);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaCarga> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaCarga.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaCarga auditoria = auditoriaCaptor.getValue();
        assertEquals("DELETE_PERMANENT", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar permanentemente carga que no existe")
    void eliminarPermanente_DebeLanzarExcepcion_CuandoNoExiste() {
        // Arrange
        Long cargaId = 999L;

        when(cargaRepository.findById(cargaId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> cargaService.eliminarPermanente(cargaId));

        assertEquals("Carga no encontrada con ID: 999", ex.getMessage());
        verify(cargaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe listar historial de cargas por transportista")
    void listarHistorialPorTransportista_DebeRetornarCargas() {
        // Arrange
        Long transportistaId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Carga> cargaPage = new PageImpl<>(List.of(carga));

        when(transportistaRepository.existsById(transportistaId)).thenReturn(true);
        when(cargaRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId, pageable))
                .thenReturn(cargaPage);

        // Act
        Page<CargaResponseDTO> resultado = cargaService.listarHistorialPorTransportista(transportistaId, pageable);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertEquals("Juan Perez", resultado.getContent().get(0).getTransportistaNombre());

        verify(transportistaRepository).existsById(transportistaId);
        verify(cargaRepository).findByTransportistaIdAndDeletedAtIsNull(transportistaId, pageable);
    }

    @Test
    @DisplayName("Debe lanzar excepción al listar historial si transportista no existe")
    void listarHistorialPorTransportista_DebeLanzarExcepcion_SiTransportistaNoExiste() {
        // Arrange
        Long transportistaId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        when(transportistaRepository.existsById(transportistaId)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> cargaService.listarHistorialPorTransportista(transportistaId, pageable));

        assertEquals("Transportista no encontrado con ID: 999", ex.getMessage());
        verify(cargaRepository, never()).findByTransportistaIdAndDeletedAtIsNull(anyLong(), any(Pageable.class));
    }
}