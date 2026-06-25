package com.example.backendastramaco.unitarias;

import com.example.backendastramaco.dto.PedidoRequestDTO;
import com.example.backendastramaco.dto.PedidoResponseDTO;
import com.example.backendastramaco.exception.ResourceNotFoundException;
import com.example.backendastramaco.model.Carga;
import com.example.backendastramaco.model.Pedido;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.audit.AuditoriaPedido;
import com.example.backendastramaco.model.enums.EstadoPedido;
import com.example.backendastramaco.model.enums.TipoMaterial;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.CargaRepository;
import com.example.backendastramaco.repository.PedidoRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.audit.AuditoriaPedidoRepository;
import com.example.backendastramaco.service.PedidoService;
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
class PedidoServiceUnitTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private TransportistaRepository transportistaRepository;

    @Mock
    private CargaRepository cargaRepository;

    @Mock
    private AuditoriaPedidoRepository auditoriaRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PedidoService pedidoService;

    private Pedido pedido;
    private PedidoRequestDTO requestDTO;
    private Transportista transportista;

    @BeforeEach
    void setUp() {
        // Configurar transportista base
        transportista = new Transportista();
        ReflectionTestUtils.setField(transportista, "id", 1L);
        transportista.setNombre("Luis");
        transportista.setApellidos("Quispe");
        transportista.setTipoTransporte(TipoTransporte.VOLQUETERO);

        // Configurar pedido base
        pedido = Pedido.builder()
                .clienteNombre("Carlos")
                .clienteTelefono("999888777")
                .direccionEnvio("Av. Principal 123")
                .tipoTransporte(TipoTransporte.VOLQUETERO)
                .material(TipoMaterial.PIEDRA)
                .cantidad(8.0)
                .montoTotal(500.0)
                .adelanto(100.0)
                .piso(2)
                .horaEnvio(LocalDateTime.of(2026, 4, 22, 10, 30))
                .transportista(transportista)
                .codigoVerificacion("1234")
                .estado(EstadoPedido.EN_ENVIO)
                .build();
        ReflectionTestUtils.setField(pedido, "id", 1L);

        // Configurar DTO
        requestDTO = new PedidoRequestDTO();
        requestDTO.setClienteNombre("Carlos");
        requestDTO.setClienteTelefono("999888777");
        requestDTO.setDireccionEnvio("Av. Principal 123");
        requestDTO.setTipoTransporte(TipoTransporte.VOLQUETERO);
        requestDTO.setMaterial(TipoMaterial.PIEDRA);
        requestDTO.setCantidad(8.0);
        requestDTO.setMontoTotal(500.0);
        requestDTO.setAdelanto(100.0);
        requestDTO.setPiso(2);
        requestDTO.setHoraEnvio(LocalDateTime.of(2026, 4, 22, 10, 30));
        requestDTO.setTransportistaId(1L);

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
    @DisplayName("Debe crear pedido para transportista volquetero sin procesar carga")
    void crearPedido_DebeCrearPedidoVolqueteroCorrectamente() {
        // Arrange
        when(transportistaRepository.findById(1L)).thenReturn(Optional.of(transportista));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 10L);
            return p;
        });
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        PedidoResponseDTO resultado = pedidoService.crearPedido(requestDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals("Carlos", resultado.getClienteNombre());
        assertEquals("999888777", resultado.getClienteTelefono());
        assertEquals("Av. Principal 123", resultado.getDireccionEnvio());
        assertEquals(TipoTransporte.VOLQUETERO, resultado.getTipoTransporte());
        assertEquals(TipoMaterial.PIEDRA, resultado.getMaterial());
        assertEquals(8.0, resultado.getCantidad());
        assertEquals(500.0, resultado.getMontoTotal());
        assertEquals(100.0, resultado.getAdelanto());
        assertEquals(2, resultado.getPiso());
        assertEquals(LocalDateTime.of(2026, 4, 22, 10, 30), resultado.getHoraEnvio());
        assertEquals(1L, resultado.getTransportistaId());
        assertEquals("Luis Quispe", resultado.getTransportistaNombre());
        assertEquals("1234", resultado.getCodigoVerificacion());

        verify(transportistaRepository).findById(1L);
        verify(pedidoRepository).save(any(Pedido.class));
        verify(cargaRepository, never()).findByTransportistaId(anyLong());
        verify(cargaRepository, never()).save(any(Carga.class));

        // Verificar auditoría
        ArgumentCaptor<AuditoriaPedido> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaPedido.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaPedido auditoria = auditoriaCaptor.getValue();
        assertEquals("CREATE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
        assertEquals(10L, auditoria.getPedidoId());
    }

    @Test
    @DisplayName("Debe crear pedido camionero y descontar stock de la carga")
    void crearPedido_DebeCrearPedidoCamioneroYDescontarCarga() {
        // Arrange
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setClienteNombre("Maria");
        dto.setClienteTelefono("999111222");
        dto.setDireccionEnvio("Jr. Lima 456");
        dto.setTipoTransporte(TipoTransporte.CAMIONERO);
        dto.setMaterial(TipoMaterial.PANDERETA);
        dto.setCantidad(20.0);
        dto.setMontoTotal(1200.0);
        dto.setAdelanto(300.0);
        dto.setPiso(1);
        dto.setHoraEnvio(LocalDateTime.of(2026, 4, 22, 14, 0));
        dto.setTransportistaId(2L);

        Transportista transportistaCamionero = new Transportista();
        ReflectionTestUtils.setField(transportistaCamionero, "id", 2L);
        transportistaCamionero.setNombre("Juan");
        transportistaCamionero.setApellidos("Perez");
        transportistaCamionero.setTipoTransporte(TipoTransporte.CAMIONERO);

        Carga carga = new Carga();
        ReflectionTestUtils.setField(carga, "id", 5L);
        carga.setTipoMaterial(TipoMaterial.PANDERETA);
        carga.setCantidadDisponible(100.0);
        carga.setTransportista(transportistaCamionero);

        when(transportistaRepository.findById(2L)).thenReturn(Optional.of(transportistaCamionero));
        when(cargaRepository.findByTransportistaId(2L)).thenReturn(Optional.of(carga));
        when(cargaRepository.save(any(Carga.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 20L);
            return p;
        });
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        PedidoResponseDTO resultado = pedidoService.crearPedido(dto);

        // Assert
        assertNotNull(resultado);
        assertEquals(20L, resultado.getId());
        assertEquals("Juan Perez", resultado.getTransportistaNombre());
        assertEquals("1234", resultado.getCodigoVerificacion());

        ArgumentCaptor<Carga> cargaCaptor = ArgumentCaptor.forClass(Carga.class);
        verify(cargaRepository).save(cargaCaptor.capture());

        Carga cargaActualizada = cargaCaptor.getValue();
        assertEquals(80.0, cargaActualizada.getCantidadDisponible());

        verify(pedidoRepository).save(any(Pedido.class));

        // Verificar auditoría
        ArgumentCaptor<AuditoriaPedido> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaPedido.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaPedido auditoria = auditoriaCaptor.getValue();
        assertEquals("CREATE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
    }

    @Test
    @DisplayName("Debe actualizar estado del pedido correctamente")
    void actualizarEstado_DebeActualizarEstadoYAuditar() {
        // Arrange
        Long pedidoId = 1L;
        String nuevoEstado = "ENTREGADO";

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        PedidoResponseDTO resultado = pedidoService.cambiarEstado(pedidoId, nuevoEstado);

        // Assert
        assertNotNull(resultado);
        assertEquals(EstadoPedido.ENTREGADO, resultado.getEstado());

        verify(pedidoRepository).findById(pedidoId);
        verify(pedidoRepository).save(pedido);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaPedido> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaPedido.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaPedido auditoria = auditoriaCaptor.getValue();
        assertEquals("UPDATE_ESTADO", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
        assertEquals("EN_ENVIO", auditoria.getEstadoAnterior());
        assertEquals("ENTREGADO", auditoria.getEstadoNuevo());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el estado es inválido")
    void actualizarEstado_DebeLanzarExcepcion_CuandoEstadoInvalido() {
        // Arrange
        Long pedidoId = 1L;
        String nuevoEstadoInvalido = "ESTADO_INEXISTENTE";

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.cambiarEstado(pedidoId, nuevoEstadoInvalido));

        verify(pedidoRepository).findById(pedidoId);
        verify(pedidoRepository, never()).save(any());
        verify(auditoriaRepository, never()).save(any(AuditoriaPedido.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el estado es null")
    void actualizarEstado_DebeLanzarExcepcion_CuandoEstadoNull() {
        // Arrange
        Long pedidoId = 1L;

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        // Act & Assert - espera NullPointerException
        assertThrows(NullPointerException.class,
                () -> pedidoService.cambiarEstado(pedidoId, null));

        verify(pedidoRepository).findById(pedidoId);
        verify(pedidoRepository, never()).save(any());
        verify(auditoriaRepository, never()).save(any(AuditoriaPedido.class));
    }

    @Test
    @DisplayName("Debe permitir actualizar a todos los estados válidos")
    void actualizarEstado_DebePermitirTodosLosEstadosValidos() {
        // Arrange
        Long pedidoId = 1L;
        List<String> estadosValidos = List.of("EN_ENVIO", "ENTREGADO", "CANCELADO");

        for (String nuevoEstado : estadosValidos) {
            Pedido pedidoTemp = Pedido.builder()
                    .clienteNombre("Cliente")
                    .estado(EstadoPedido.EN_ENVIO)
                    .build();
            ReflectionTestUtils.setField(pedidoTemp, "id", pedidoId);

            when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedidoTemp));
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            // Act
            PedidoResponseDTO resultado = pedidoService.cambiarEstado(pedidoId, nuevoEstado);

            // Assert
            assertNotNull(resultado);
            assertEquals(EstadoPedido.valueOf(nuevoEstado), resultado.getEstado());
        }
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando no se encuentra el transportista")
    void crearPedido_DebeLanzarExcepcionCuandoTransportistaNoExiste() {
        // Arrange
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setTransportistaId(99L);

        when(transportistaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> pedidoService.crearPedido(dto));

        assertEquals("Transportista no encontrado con ID: 99", ex.getMessage());

        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(cargaRepository, never()).save(any(Carga.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaPedido.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el tipo de transporte no coincide")
    void crearPedido_DebeLanzarExcepcionCuandoTipoNoCoincide() {
        // Arrange
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setTransportistaId(1L);
        dto.setTipoTransporte(TipoTransporte.CAMIONERO);

        Transportista transportistaVolquetero = new Transportista();
        ReflectionTestUtils.setField(transportistaVolquetero, "id", 1L);
        transportistaVolquetero.setTipoTransporte(TipoTransporte.VOLQUETERO);

        when(transportistaRepository.findById(1L)).thenReturn(Optional.of(transportistaVolquetero));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.crearPedido(dto));

        assertEquals("El tipo de transporte del pedido no coincide con el transportista seleccionado", ex.getMessage());

        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaPedido.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando camionero usa material no permitido")
    void crearPedido_DebeLanzarExcepcionCuandoMaterialCamioneroEsInvalido() {
        // Arrange
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setTransportistaId(2L);
        dto.setTipoTransporte(TipoTransporte.CAMIONERO);
        dto.setMaterial(TipoMaterial.ARENA_FINA);
        dto.setCantidad(10.0);

        Transportista transportistaCamionero = new Transportista();
        ReflectionTestUtils.setField(transportistaCamionero, "id", 2L);
        transportistaCamionero.setTipoTransporte(TipoTransporte.CAMIONERO);

        when(transportistaRepository.findById(2L)).thenReturn(Optional.of(transportistaCamionero));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pedidoService.crearPedido(dto)
        );

        assertEquals(
                "El transportista camionero solo puede trabajar con materiales PANDERETA o TECHO",
                ex.getMessage()
        );

        verify(auditoriaRepository, never()).save(any(AuditoriaPedido.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el camionero no tiene carga registrada")
    void crearPedido_DebeLanzarExcepcionCuandoNoTieneCargaRegistrada() {
        // Arrange
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setTransportistaId(2L);
        dto.setTipoTransporte(TipoTransporte.CAMIONERO);
        dto.setMaterial(TipoMaterial.PANDERETA);
        dto.setCantidad(10.0);

        Transportista transportistaCamionero = new Transportista();
        ReflectionTestUtils.setField(transportistaCamionero, "id", 2L);
        transportistaCamionero.setTipoTransporte(TipoTransporte.CAMIONERO);

        when(transportistaRepository.findById(2L)).thenReturn(Optional.of(transportistaCamionero));
        when(cargaRepository.findByTransportistaId(2L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pedidoService.crearPedido(dto));

        assertEquals("El transportista no tiene carga registrada", ex.getMessage());

        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaPedido.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el material de la carga no coincide")
    void crearPedido_DebeLanzarExcepcionCuandoMaterialNoCoincideConCarga() {
        // Arrange
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setTransportistaId(2L);
        dto.setTipoTransporte(TipoTransporte.CAMIONERO);
        dto.setMaterial(TipoMaterial.PANDERETA);
        dto.setCantidad(10.0);

        Transportista transportistaCamionero = new Transportista();
        ReflectionTestUtils.setField(transportistaCamionero, "id", 2L);
        transportistaCamionero.setTipoTransporte(TipoTransporte.CAMIONERO);

        Carga carga = new Carga();
        carga.setTipoMaterial(TipoMaterial.TECHO);
        carga.setCantidadDisponible(50.0);

        when(transportistaRepository.findById(2L)).thenReturn(Optional.of(transportistaCamionero));
        when(cargaRepository.findByTransportistaId(2L)).thenReturn(Optional.of(carga));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.crearPedido(dto));

        assertEquals("El transportista no cuenta con ese material en su carga actual", ex.getMessage());

        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(cargaRepository, never()).save(any(Carga.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaPedido.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el stock es insuficiente")
    void crearPedido_DebeLanzarExcepcionCuandoStockEsInsuficiente() {
        // Arrange
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setTransportistaId(2L);
        dto.setTipoTransporte(TipoTransporte.CAMIONERO);
        dto.setMaterial(TipoMaterial.PANDERETA);
        dto.setCantidad(60.0);

        Transportista transportistaCamionero = new Transportista();
        ReflectionTestUtils.setField(transportistaCamionero, "id", 2L);
        transportistaCamionero.setTipoTransporte(TipoTransporte.CAMIONERO);

        Carga carga = new Carga();
        carga.setTipoMaterial(TipoMaterial.PANDERETA);
        carga.setCantidadDisponible(30.0);

        when(transportistaRepository.findById(2L)).thenReturn(Optional.of(transportistaCamionero));
        when(cargaRepository.findByTransportistaId(2L)).thenReturn(Optional.of(carga));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.crearPedido(dto));

        assertEquals("Stock insuficiente para atender el pedido", ex.getMessage());

        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(cargaRepository, never()).save(any(Carga.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaPedido.class));
    }

    @Test
    @DisplayName("Debe listar pedidos con paginación")
    void listar_DebeRetornarPaginaDePedidos() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> pedidoPage = new PageImpl<>(List.of(pedido));
        when(pedidoRepository.findByDeletedAtIsNull(pageable)).thenReturn(pedidoPage);

        // Act
        Page<PedidoResponseDTO> resultado = pedidoService.listar(pageable, null, null);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertEquals("Carlos", resultado.getContent().get(0).getClienteNombre());
        assertEquals("Luis Quispe", resultado.getContent().get(0).getTransportistaNombre());

        verify(pedidoRepository).findByDeletedAtIsNull(pageable);
    }

    @Test
    @DisplayName("Debe listar pedidos con filtro por estado")
    void listar_DebeFiltrarPorEstado() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> pedidoPage = new PageImpl<>(List.of(pedido));
        when(pedidoRepository.findByEstadoAndDeletedAtIsNull(EstadoPedido.EN_ENVIO, pageable))
                .thenReturn(pedidoPage);

        // Act
        Page<PedidoResponseDTO> resultado = pedidoService.listar(pageable, "EN_ENVIO", null);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        verify(pedidoRepository).findByEstadoAndDeletedAtIsNull(EstadoPedido.EN_ENVIO, pageable);
    }

    @Test
    @DisplayName("Debe listar pedidos por transportista con paginación")
    void listarPorTransportista_DebeRetornarPaginaDePedidos() {
        // Arrange
        Long transportistaId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> pedidoPage = new PageImpl<>(List.of(pedido));
        when(pedidoRepository.findByTransportistaIdAndDeletedAtIsNull(transportistaId, pageable))
                .thenReturn(pedidoPage);

        // Act
        Page<PedidoResponseDTO> resultado = pedidoService.listarPorTransportista(transportistaId, pageable, null);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertEquals("Carlos", resultado.getContent().get(0).getClienteNombre());

        verify(pedidoRepository).findByTransportistaIdAndDeletedAtIsNull(transportistaId, pageable);
    }

    @Test
    @DisplayName("Debe obtener pedido por ID")
    void obtenerPorId_DebeRetornarPedido_CuandoExiste() {
        // Arrange
        Long pedidoId = 1L;
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        // Act
        PedidoResponseDTO resultado = pedidoService.obtenerPorId(pedidoId);

        // Assert
        assertNotNull(resultado);
        assertEquals(pedidoId, resultado.getId());
        assertEquals("Carlos", resultado.getClienteNombre());

        verify(pedidoRepository).findById(pedidoId);
    }

    @Test
    @DisplayName("Debe lanzar excepción al obtener pedido por ID que no existe")
    void obtenerPorId_DebeLanzarExcepcion_CuandoNoExiste() {
        // Arrange
        Long pedidoId = 999L;
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> pedidoService.obtenerPorId(pedidoId));

        assertEquals("Pedido no encontrado con ID: 999", ex.getMessage());
    }

    @Test
    @DisplayName("Debe actualizar pedido exitosamente")
    void actualizar_DebeActualizarPedidoYAuditar() {
        // Arrange
        Long pedidoId = 1L;

        PedidoRequestDTO updateDTO = new PedidoRequestDTO();
        updateDTO.setClienteNombre("Carlos Actualizado");
        updateDTO.setClienteTelefono("888777666");
        updateDTO.setDireccionEnvio("Nueva Direccion 456");
        updateDTO.setTipoTransporte(TipoTransporte.VOLQUETERO);
        updateDTO.setMaterial(TipoMaterial.ARENA_ASENTAR);
        updateDTO.setCantidad(15.0);
        updateDTO.setMontoTotal(750.0);
        updateDTO.setAdelanto(200.0);
        updateDTO.setPiso(3);
        updateDTO.setHoraEnvio(LocalDateTime.of(2026, 4, 23, 11, 0));
        updateDTO.setTransportistaId(1L);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(transportistaRepository.findById(1L)).thenReturn(Optional.of(transportista));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        PedidoResponseDTO resultado = pedidoService.actualizar(pedidoId, updateDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals("Carlos Actualizado", resultado.getClienteNombre());
        assertEquals("888777666", resultado.getClienteTelefono());
        assertEquals("Nueva Direccion 456", resultado.getDireccionEnvio());
        assertEquals(TipoMaterial.ARENA_ASENTAR, resultado.getMaterial());
        assertEquals(15.0, resultado.getCantidad());

        verify(pedidoRepository).findById(pedidoId);
        verify(pedidoRepository).save(any(Pedido.class));

        // Verificar auditoría
        ArgumentCaptor<AuditoriaPedido> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaPedido.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaPedido auditoria = auditoriaCaptor.getValue();
        assertEquals("UPDATE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
        assertEquals("Carlos", auditoria.getClienteNombreAnterior());
        assertEquals("Carlos Actualizado", auditoria.getClienteNombreNuevo());
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar pedido que no existe")
    void actualizar_DebeLanzarExcepcion_CuandoPedidoNoExiste() {
        // Arrange
        Long pedidoId = 999L;
        PedidoRequestDTO updateDTO = new PedidoRequestDTO();

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> pedidoService.actualizar(pedidoId, updateDTO));

        assertEquals("Pedido no encontrado con ID: 999", ex.getMessage());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Debe eliminar pedido lógicamente (soft delete)")
    void eliminar_DebeEliminarPedidoLogicamente() {
        // Arrange
        Long pedidoId = 1L;
        String username = "admin";

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        pedidoService.eliminar(pedidoId, username);

        // Assert
        assertNotNull(pedido.getDeletedAt());
        assertEquals(username, pedido.getDeletedBy());

        verify(pedidoRepository).findById(pedidoId);
        verify(pedidoRepository).save(pedido);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaPedido> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaPedido.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaPedido auditoria = auditoriaCaptor.getValue();
        assertEquals("DELETE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar pedido que ya está eliminado")
    void eliminar_DebeLanzarExcepcion_CuandoPedidoYaEliminado() {
        // Arrange
        Long pedidoId = 1L;
        pedido.setDeletedAt(LocalDateTime.now());
        pedido.setDeletedBy("admin");

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pedidoService.eliminar(pedidoId, "admin"));

        assertEquals("El pedido ya está eliminado", ex.getMessage());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar pedido que no existe")
    void eliminar_DebeLanzarExcepcion_CuandoPedidoNoExiste() {
        // Arrange
        Long pedidoId = 999L;

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> pedidoService.eliminar(pedidoId, "admin"));

        assertEquals("Pedido no encontrado con ID: 999", ex.getMessage());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Debe restaurar pedido eliminado lógicamente")
    void restaurar_DebeRestaurarPedidoEliminado() {
        // Arrange
        Long pedidoId = 1L;
        pedido.setDeletedAt(LocalDateTime.now());
        pedido.setDeletedBy("admin");

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        pedidoService.restaurar(pedidoId);

        // Assert
        assertNull(pedido.getDeletedAt());
        assertNull(pedido.getDeletedBy());

        verify(pedidoRepository).save(pedido);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaPedido> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaPedido.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaPedido auditoria = auditoriaCaptor.getValue();
        assertEquals("RESTORE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
    }

    @Test
    @DisplayName("Debe lanzar excepción al restaurar pedido no eliminado")
    void restaurar_DebeLanzarExcepcion_CuandoPedidoNoEliminado() {
        // Arrange
        Long pedidoId = 1L;
        pedido.setDeletedAt(null);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pedidoService.restaurar(pedidoId));

        assertEquals("El pedido no está eliminado", ex.getMessage());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Debe eliminar permanentemente un pedido")
    void eliminarPermanente_DebeEliminarPedidoFisicamente() {
        // Arrange
        Long pedidoId = 1L;

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        pedidoService.eliminarPermanente(pedidoId);

        // Assert
        verify(pedidoRepository).delete(pedido);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaPedido> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaPedido.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaPedido auditoria = auditoriaCaptor.getValue();
        assertEquals("DELETE_PERMANENT", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar permanentemente pedido que no existe")
    void eliminarPermanente_DebeLanzarExcepcion_CuandoNoExiste() {
        // Arrange
        Long pedidoId = 999L;

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> pedidoService.eliminarPermanente(pedidoId));

        assertEquals("Pedido no encontrado con ID: 999", ex.getMessage());
        verify(pedidoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe retornar nombre completo sin espacios extra cuando transportista tiene espacios")
    void toResponseDTO_DebeMapearNombreCompletoSinEspaciosExtra() {
        // Arrange
        Transportista transportistaConEspacios = new Transportista();
        ReflectionTestUtils.setField(transportistaConEspacios, "id", 8L);
        transportistaConEspacios.setNombre("  Joel ");
        transportistaConEspacios.setApellidos(" ");

        Pedido pedidoConEspacios = Pedido.builder()
                .clienteNombre("Mario")
                .clienteTelefono("999999999")
                .direccionEnvio("Jr. Sol")
                .tipoTransporte(TipoTransporte.VOLQUETERO)
                .material(TipoMaterial.ARENA_ASENTAR)
                .cantidad(4.0)
                .montoTotal(120.0)
                .adelanto(20.0)
                .piso(1)
                .horaEnvio(LocalDateTime.of(2026, 4, 22, 18, 0))
                .transportista(transportistaConEspacios)
                .codigoVerificacion("2222")
                .build();
        ReflectionTestUtils.setField(pedidoConEspacios, "id", 9L);

        when(pedidoRepository.findByDeletedAtIsNull(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pedidoConEspacios)));

        // Act
        Page<PedidoResponseDTO> resultado = pedidoService.listar(PageRequest.of(0, 10), null, null);

        // Assert
        assertEquals(1, resultado.getContent().size());
        assertEquals("Joel", resultado.getContent().get(0).getTransportistaNombre());
    }

    @Test
    @DisplayName("Debe listar pedidos eliminados")
    void listarEliminados_DebeRetornarPedidosEliminados() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        pedido.setDeletedAt(LocalDateTime.now());
        Page<Pedido> pedidoPage = new PageImpl<>(List.of(pedido));
        when(pedidoRepository.findByDeletedAtIsNotNull(pageable)).thenReturn(pedidoPage);

        // Act
        Page<PedidoResponseDTO> resultado = pedidoService.listarEliminados(pageable);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertNotNull(resultado.getContent().get(0).getEstado());

        verify(pedidoRepository).findByDeletedAtIsNotNull(pageable);
    }

    @Test
    @DisplayName("Debe listar todos los pedidos incluyendo eliminados")
    void listarTodos_DebeRetornarTodosLosPedidos() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> pedidoPage = new PageImpl<>(List.of(pedido, new Pedido()));
        when(pedidoRepository.findAll(pageable)).thenReturn(pedidoPage);

        // Act
        Page<PedidoResponseDTO> resultado = pedidoService.listarTodos(pageable);

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.getContent().size());

        verify(pedidoRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Debe listar pedidos con filtro por transportistaId y estado")
    void listar_DebeFiltrarPorTransportistaYEstado() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> pedidoPage = new PageImpl<>(List.of(pedido));
        when(pedidoRepository.findByEstadoAndTransportistaIdAndDeletedAtIsNull(
                EstadoPedido.EN_ENVIO, 1L, pageable))
                .thenReturn(pedidoPage);

        // Act
        Page<PedidoResponseDTO> resultado = pedidoService.listar(pageable, "EN_ENVIO", 1L);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        verify(pedidoRepository).findByEstadoAndTransportistaIdAndDeletedAtIsNull(
                EstadoPedido.EN_ENVIO, 1L, pageable);
    }
}