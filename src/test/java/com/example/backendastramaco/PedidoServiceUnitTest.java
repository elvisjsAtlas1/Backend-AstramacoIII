package com.example.backendastramaco;

import com.example.backendastramaco.dto.PedidoRequestDTO;
import com.example.backendastramaco.dto.PedidoResponseDTO;
import com.example.backendastramaco.model.Carga;
import com.example.backendastramaco.model.Pedido;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.enums.TipoMaterial;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.CargaRepository;
import com.example.backendastramaco.repository.PedidoRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.service.PedidoService;
import com.example.backendastramaco.service.audit.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceUnitTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private TransportistaRepository transportistaRepository;

    @Mock
    private CargaRepository cargaRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PedidoService pedidoService;

    @Test
    @DisplayName("Debe crear pedido para transportista volquetero sin procesar carga")
    void crearPedido_DebeCrearPedidoVolqueteroCorrectamente() {
        // Arrange
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setClienteNombre("Carlos");
        dto.setClienteTelefono("999888777");
        dto.setDireccionEnvio("Av. Principal 123");
        dto.setTipoTransporte(TipoTransporte.VOLQUETERO);
        dto.setMaterial(TipoMaterial.PIEDRA);
        dto.setCantidad(8.0);
        dto.setMontoTotal(500.0);
        dto.setAdelanto(100.0);
        dto.setPiso(2);
        dto.setHoraEnvio(LocalDateTime.of(2026, 4, 22, 10, 30));
        dto.setTransportistaId(1L);

        Transportista transportista = new Transportista();
        ReflectionTestUtils.setField(transportista, "id", 1L);
        transportista.setNombre("Luis");
        transportista.setApellidos("Quispe");
        transportista.setTipoTransporte(TipoTransporte.VOLQUETERO);

        when(transportistaRepository.findById(1L)).thenReturn(Optional.of(transportista));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            ReflectionTestUtils.setField(pedido, "id", 10L);
            return pedido;
        });

        doNothing().when(auditService).auditPedido(anyLong(), anyString(), any(), any(), any());

        // Act
        PedidoResponseDTO resultado = pedidoService.crearPedido(dto);

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
        verify(auditService, times(1)).auditPedido(anyLong(), eq("CREATE"), isNull(), any(), any());
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

        Transportista transportista = new Transportista();
        ReflectionTestUtils.setField(transportista, "id", 2L);
        transportista.setNombre("Juan");
        transportista.setApellidos("Perez");
        transportista.setTipoTransporte(TipoTransporte.CAMIONERO);

        Carga carga = new Carga();
        ReflectionTestUtils.setField(carga, "id", 5L);
        carga.setTipoMaterial(TipoMaterial.PANDERETA);
        carga.setCantidadDisponible(100.0);
        carga.setTransportista(transportista);

        when(transportistaRepository.findById(2L)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaId(2L)).thenReturn(Optional.of(carga));
        when(cargaRepository.save(any(Carga.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            ReflectionTestUtils.setField(pedido, "id", 20L);
            return pedido;
        });

        doNothing().when(auditService).auditPedido(anyLong(), anyString(), any(), any(), any());

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
        verify(auditService, times(1)).auditPedido(anyLong(), eq("CREATE"), isNull(), any(), any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando no se encuentra el transportista")
    void crearPedido_DebeLanzarExcepcionCuandoTransportistaNoExiste() {
        // Arrange
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setTransportistaId(99L);

        when(transportistaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pedidoService.crearPedido(dto));

        assertEquals("Transportista no encontrado", ex.getMessage());

        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(cargaRepository, never()).save(any(Carga.class));
        verify(auditService, never()).auditPedido(anyLong(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el tipo de transporte no coincide")
    void crearPedido_DebeLanzarExcepcionCuandoTipoNoCoincide() {
        // Arrange
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setTransportistaId(1L);
        dto.setTipoTransporte(TipoTransporte.CAMIONERO);

        Transportista transportista = new Transportista();
        ReflectionTestUtils.setField(transportista, "id", 1L);
        transportista.setTipoTransporte(TipoTransporte.VOLQUETERO);

        when(transportistaRepository.findById(1L)).thenReturn(Optional.of(transportista));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.crearPedido(dto));

        assertEquals("El tipo de transporte del pedido no coincide con el transportista seleccionado", ex.getMessage());

        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(auditService, never()).auditPedido(anyLong(), anyString(), any(), any(), any());
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

        Transportista transportista = new Transportista();
        ReflectionTestUtils.setField(transportista, "id", 2L);
        transportista.setTipoTransporte(TipoTransporte.CAMIONERO);

        when(transportistaRepository.findById(2L)).thenReturn(Optional.of(transportista));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pedidoService.crearPedido(dto)
        );

        assertEquals(
                "El transportista camionero solo puede trabajar con materiales PANDERETA o TECHO",
                ex.getMessage()
        );

        verify(auditService, never()).auditPedido(anyLong(), anyString(), any(), any(), any());
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

        Transportista transportista = new Transportista();
        ReflectionTestUtils.setField(transportista, "id", 2L);
        transportista.setTipoTransporte(TipoTransporte.CAMIONERO);

        when(transportistaRepository.findById(2L)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaId(2L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pedidoService.crearPedido(dto));

        assertEquals("El transportista no tiene carga registrada", ex.getMessage());

        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(auditService, never()).auditPedido(anyLong(), anyString(), any(), any(), any());
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

        Transportista transportista = new Transportista();
        ReflectionTestUtils.setField(transportista, "id", 2L);
        transportista.setTipoTransporte(TipoTransporte.CAMIONERO);

        Carga carga = new Carga();
        carga.setTipoMaterial(TipoMaterial.TECHO);
        carga.setCantidadDisponible(50.0);

        when(transportistaRepository.findById(2L)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaId(2L)).thenReturn(Optional.of(carga));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.crearPedido(dto));

        assertEquals("El transportista no cuenta con ese material en su carga actual", ex.getMessage());

        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(cargaRepository, never()).save(any(Carga.class));
        verify(auditService, never()).auditPedido(anyLong(), anyString(), any(), any(), any());
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

        Transportista transportista = new Transportista();
        ReflectionTestUtils.setField(transportista, "id", 2L);
        transportista.setTipoTransporte(TipoTransporte.CAMIONERO);

        Carga carga = new Carga();
        carga.setTipoMaterial(TipoMaterial.PANDERETA);
        carga.setCantidadDisponible(30.0);

        when(transportistaRepository.findById(2L)).thenReturn(Optional.of(transportista));
        when(cargaRepository.findByTransportistaId(2L)).thenReturn(Optional.of(carga));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.crearPedido(dto));

        assertEquals("Stock insuficiente para atender el pedido", ex.getMessage());

        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(cargaRepository, never()).save(any(Carga.class));
        verify(auditService, never()).auditPedido(anyLong(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe listar pedidos convertidos a response DTO")
    void listar_DebeRetornarPedidosConvertidosADto() {
        // Arrange
        Transportista transportista = new Transportista();
        ReflectionTestUtils.setField(transportista, "id", 3L);
        transportista.setNombre("Pedro");
        transportista.setApellidos("Mamani");

        Pedido pedido1 = Pedido.builder()
                .clienteNombre("Cliente 1")
                .clienteTelefono("111111111")
                .direccionEnvio("Dir 1")
                .tipoTransporte(TipoTransporte.VOLQUETERO)
                .material(TipoMaterial.ARENA_ASENTAR)
                .cantidad(5.0)
                .montoTotal(200.0)
                .adelanto(50.0)
                .piso(1)
                .horaEnvio(LocalDateTime.of(2026, 4, 22, 9, 0))
                .transportista(transportista)
                .codigoVerificacion("1234")
                .build();
        ReflectionTestUtils.setField(pedido1, "id", 1L);

        Pedido pedido2 = Pedido.builder()
                .clienteNombre("Cliente 2")
                .clienteTelefono("222222222")
                .direccionEnvio("Dir 2")
                .tipoTransporte(TipoTransporte.CAMIONERO)
                .material(TipoMaterial.PANDERETA)
                .cantidad(10.0)
                .montoTotal(400.0)
                .adelanto(100.0)
                .piso(2)
                .horaEnvio(LocalDateTime.of(2026, 4, 22, 11, 0))
                .transportista(transportista)
                .codigoVerificacion("5678")
                .build();
        ReflectionTestUtils.setField(pedido2, "id", 2L);

        when(pedidoRepository.findAll()).thenReturn(List.of(pedido1, pedido2));

        // Act
        List<PedidoResponseDTO> resultado = pedidoService.listar();

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals(1L, resultado.get(0).getId());
        assertEquals("Cliente 1", resultado.get(0).getClienteNombre());
        assertEquals("Pedro Mamani", resultado.get(0).getTransportistaNombre());
        assertEquals(2L, resultado.get(1).getId());
        assertEquals("Cliente 2", resultado.get(1).getClienteNombre());
        assertEquals("Pedro Mamani", resultado.get(1).getTransportistaNombre());

        verify(pedidoRepository).findAll();
    }

    @Test
    @DisplayName("Debe listar pedidos por transportista ordenados por hora de envío")
    void listarPorTransportista_DebeRetornarPedidosDelTransportista() {
        // Arrange
        Transportista transportista = new Transportista();
        ReflectionTestUtils.setField(transportista, "id", 4L);
        transportista.setNombre("Rene");
        transportista.setApellidos("Flores");

        Pedido pedido = Pedido.builder()
                .clienteNombre("Lucia")
                .clienteTelefono("987654321")
                .direccionEnvio("Av. Siempre Viva")
                .tipoTransporte(TipoTransporte.VOLQUETERO)
                .material(TipoMaterial.ARENA_ASENTAR)
                .cantidad(12.0)
                .montoTotal(800.0)
                .adelanto(200.0)
                .piso(3)
                .horaEnvio(LocalDateTime.of(2026, 4, 22, 16, 0))
                .transportista(transportista)
                .codigoVerificacion("1234")
                .build();
        ReflectionTestUtils.setField(pedido, "id", 7L);

        when(pedidoRepository.findByTransportistaIdOrderByHoraEnvioDesc(4L))
                .thenReturn(List.of(pedido));

        // Act
        List<PedidoResponseDTO> resultado = pedidoService.listarPorTransportista(4L);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(7L, resultado.get(0).getId());
        assertEquals("Lucia", resultado.get(0).getClienteNombre());
        assertEquals("Rene Flores", resultado.get(0).getTransportistaNombre());

        verify(pedidoRepository).findByTransportistaIdOrderByHoraEnvioDesc(4L);
    }

    @Test
    @DisplayName("Debe retornar nombre completo vacío limpio cuando transportista no tiene apellidos o nombre completos")
    void listar_DebeMapearNombreCompletoSinEspaciosExtra() {
        // Arrange
        Transportista transportista = new Transportista();
        ReflectionTestUtils.setField(transportista, "id", 8L);
        transportista.setNombre("  Joel ");
        transportista.setApellidos(" ");

        Pedido pedido = Pedido.builder()
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
                .transportista(transportista)
                .codigoVerificacion("2222")
                .build();
        ReflectionTestUtils.setField(pedido, "id", 9L);

        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));

        // Act
        List<PedidoResponseDTO> resultado = pedidoService.listar();

        // Assert
        assertEquals(1, resultado.size());
        assertEquals("Joel", resultado.get(0).getTransportistaNombre());
    }
}