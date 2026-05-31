package com.example.backendastramaco;

import com.example.backendastramaco.dto.TransportistaRequestDTO;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.enums.EstadoTransportista;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.service.TransportistaService;
import com.example.backendastramaco.service.audit.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransportistaServiceUnitTest {

    @Mock
    private TransportistaRepository transportistaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private TransportistaService transportistaService;

    @Test
    @DisplayName("Debe crear transportista con usuario automático y estado ACTIVO por defecto")
    void crear_DebeCrearTransportistaConEstadoPorDefecto() {
        TransportistaRequestDTO dto = new TransportistaRequestDTO();
        dto.setNombre("Juan");
        dto.setApellidos("Perez");
        dto.setDni("12345678");
        dto.setEdad(30);
        dto.setTipoTransporte(TipoTransporte.CAMIONERO);
        dto.setPlaca("ABC-123");
        dto.setVehiculoInfo("Camion rojo");
        dto.setCapacidad(10.5);
        dto.setEstado(null);

        when(usuarioRepository.findByUsername("juan.perez")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("12345678")).thenReturn("clave-codificada");

        Usuario usuarioGuardado = Usuario.builder()
                .username("juan.perez")
                .password("clave-codificada")
                .rol(Rol.TRANSPORTISTA)
                .activo(true)
                .build();

        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(transportistaRepository.save(any(Transportista.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Usar any() en lugar de anyLong() porque el ID puede ser null antes de guardar
        doNothing().when(auditService).auditTransportista(any(), anyString(), any(), any(), any());

        Transportista resultado = transportistaService.crear(dto);

        assertNotNull(resultado);
        assertEquals(EstadoTransportista.ACTIVO, resultado.getEstado());
        assertNotNull(resultado.getUsuario());
        assertEquals("juan.perez", resultado.getUsuario().getUsername());
        assertEquals(Rol.TRANSPORTISTA, resultado.getUsuario().getRol());
        assertEquals(Boolean.TRUE, resultado.getUsuario().getActivo());

        verify(passwordEncoder).encode("12345678");
        verify(usuarioRepository).save(any(Usuario.class));
        verify(transportistaRepository).save(any(Transportista.class));
        verify(auditService, times(1)).auditTransportista(any(), eq("CREATE"), isNull(), any(), any());
    }

    @Test
    @DisplayName("Debe generar username único cuando el username base ya existe")
    void crear_DebeGenerarUsernameUnico() {
        TransportistaRequestDTO dto = new TransportistaRequestDTO();
        dto.setNombre("Juan");
        dto.setApellidos("Perez");
        dto.setDni("12345678");
        dto.setEdad(30);
        dto.setTipoTransporte(TipoTransporte.CAMIONERO);
        dto.setPlaca("ABC-123");
        dto.setVehiculoInfo("Camion");
        dto.setCapacidad(8.0);
        dto.setEstado("ACTIVO");

        when(usuarioRepository.findByUsername("juan.perez"))
                .thenReturn(Optional.of(mock(Usuario.class)));
        when(usuarioRepository.findByUsername("juan.perez1"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("12345678")).thenReturn("clave-codificada");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transportistaRepository.save(any(Transportista.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(auditService).auditTransportista(any(), anyString(), any(), any(), any());

        Transportista resultado = transportistaService.crear(dto);

        assertNotNull(resultado);
        assertNotNull(resultado.getUsuario());
        assertEquals("juan.perez1", resultado.getUsuario().getUsername());

        verify(auditService, times(1)).auditTransportista(any(), eq("CREATE"), isNull(), any(), any());
    }

    @Test
    @DisplayName("Debe asignar estado enviado en el DTO")
    void crear_DebeAsignarEstadoDelDto() {
        TransportistaRequestDTO dto = new TransportistaRequestDTO();
        dto.setNombre("Ana");
        dto.setApellidos("Lopez");
        dto.setDni("87654321");
        dto.setEdad(28);
        dto.setTipoTransporte(TipoTransporte.VOLQUETERO);
        dto.setPlaca("XYZ-999");
        dto.setVehiculoInfo("Unidad azul");
        dto.setCapacidad(15.0);
        dto.setEstado("INACTIVO");

        when(usuarioRepository.findByUsername("ana.lopez")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("87654321")).thenReturn("clave-codificada");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transportistaRepository.save(any(Transportista.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(auditService).auditTransportista(any(), anyString(), any(), any(), any());

        Transportista resultado = transportistaService.crear(dto);

        assertNotNull(resultado);
        assertEquals(EstadoTransportista.INACTIVO, resultado.getEstado());

        verify(auditService, times(1)).auditTransportista(any(), eq("CREATE"), isNull(), any(), any());
    }

    @Test
    @DisplayName("Debe listar transportistas activos por tipo")
    void listarPorTipo_DebeRetornarTransportistasActivos() {
        when(transportistaRepository.findByTipoTransporteAndEstado(
                TipoTransporte.CAMIONERO,
                EstadoTransportista.ACTIVO
        )).thenReturn(java.util.List.of(new Transportista(), new Transportista()));

        var resultado = transportistaService.listarPorTipo(TipoTransporte.CAMIONERO);

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
    }

    @Test
    @DisplayName("Debe listar todos los transportistas")
    void listar_DebeRetornarTodos() {
        when(transportistaRepository.findAll())
                .thenReturn(java.util.List.of(new Transportista(), new Transportista(), new Transportista()));

        var resultado = transportistaService.listar();

        assertNotNull(resultado);
        assertEquals(3, resultado.size());
    }

    // ========== PRUEBAS PARA ACTUALIZAR ==========

    @Test
    @DisplayName("Debe actualizar transportista exitosamente")
    void actualizar_DebeActualizarTransportista_CuandoExiste() {
        // Arrange
        Long transportistaId = 1L;
        Transportista transportistaExistente = new Transportista();
        transportistaExistente.setId(transportistaId);
        transportistaExistente.setNombre("Juan");
        transportistaExistente.setApellidos("Perez");
        transportistaExistente.setDni("12345678");
        transportistaExistente.setEdad(30);
        transportistaExistente.setTipoTransporte(TipoTransporte.CAMIONERO);
        transportistaExistente.setPlaca("ABC-123");
        transportistaExistente.setVehiculoInfo("Camion rojo");
        transportistaExistente.setCapacidad(10.5);
        transportistaExistente.setEstado(EstadoTransportista.ACTIVO);

        TransportistaRequestDTO dto = new TransportistaRequestDTO();
        dto.setNombre("Juan Carlos");
        dto.setApellidos("Perez Gomez");
        dto.setDni("87654321");
        dto.setEdad(35);
        dto.setTipoTransporte(TipoTransporte.VOLQUETERO);
        dto.setPlaca("XYZ-789");
        dto.setVehiculoInfo("Camion azul");
        dto.setCapacidad(15.0);
        dto.setEstado("INACTIVO");

        when(transportistaRepository.findById(transportistaId))
                .thenReturn(Optional.of(transportistaExistente));
        when(transportistaRepository.save(any(Transportista.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(auditService).auditTransportista(any(), anyString(), any(), any(), any());

        // Act
        Transportista resultado = transportistaService.actualizar(transportistaId, dto);

        // Assert
        assertNotNull(resultado);
        assertEquals("Juan Carlos", resultado.getNombre());
        assertEquals("Perez Gomez", resultado.getApellidos());
        assertEquals("87654321", resultado.getDni());
        assertEquals(35, resultado.getEdad());
        assertEquals(TipoTransporte.VOLQUETERO, resultado.getTipoTransporte());
        assertEquals("XYZ-789", resultado.getPlaca());
        assertEquals("Camion azul", resultado.getVehiculoInfo());
        assertEquals(15.0, resultado.getCapacidad());
        assertEquals(EstadoTransportista.INACTIVO, resultado.getEstado());

        verify(transportistaRepository).findById(transportistaId);
        verify(transportistaRepository).save(any(Transportista.class));
        verify(auditService, times(1)).auditTransportista(any(), eq("UPDATE"), any(), any(), any());
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar transportista que no existe")
    void actualizar_DebeLanzarExcepcion_CuandoTransportistaNoExiste() {
        // Arrange
        Long transportistaId = 999L;
        TransportistaRequestDTO dto = new TransportistaRequestDTO();

        when(transportistaRepository.findById(transportistaId))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transportistaService.actualizar(transportistaId, dto);
        });

        assertTrue(exception.getMessage().contains("Transportista no encontrado"));
        verify(transportistaRepository, never()).save(any());
    }

// ========== PRUEBAS PARA ELIMINAR ==========

    @Test
    @DisplayName("Debe eliminar transportista lógicamente (soft delete)")
    void eliminar_DebeEliminarTransportistaLogicamente_CuandoExiste() {
        // Arrange
        Long transportistaId = 1L;
        String username = "admin";
        Transportista transportistaExistente = new Transportista();
        transportistaExistente.setId(transportistaId);
        transportistaExistente.setNombre("Juan");
        transportistaExistente.setEstado(EstadoTransportista.ACTIVO);

        when(transportistaRepository.findById(transportistaId))
                .thenReturn(Optional.of(transportistaExistente));
        when(transportistaRepository.save(any(Transportista.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(auditService).auditTransportista(anyLong(), anyString(), any(), any(), any());

        // Act
        transportistaService.eliminar(transportistaId, username);

        // Assert
        assertEquals(EstadoTransportista.INACTIVO, transportistaExistente.getEstado(),
                "El estado debe cambiar a INACTIVO");
        assertNotNull(transportistaExistente.getDeletedAt(), "deletedAt debe estar presente");
        assertEquals(username, transportistaExistente.getDeletedBy(), "deletedBy debe ser el username");

        verify(transportistaRepository).findById(transportistaId);
        verify(transportistaRepository).save(transportistaExistente);
    }
// ========== PRUEBAS PARA COPIAR ENTIDAD ==========

    @Test
    @DisplayName("Debe copiar correctamente los datos del transportista sin copiar el ID")
    void copiarEntidad_DebeCopiarDatosCorrectamente() {
        // Arrange
        Transportista source = new Transportista();
        source.setId(100L);
        source.setNombre("Juan");
        source.setApellidos("Perez");
        source.setDni("12345678");
        source.setEdad(30);
        source.setTipoTransporte(TipoTransporte.CAMIONERO);
        source.setPlaca("ABC-123");
        source.setVehiculoInfo("Camion rojo");
        source.setCapacidad(10.5);
        source.setEstado(EstadoTransportista.ACTIVO);

        // Act
        Transportista result = transportistaService.copiarEntidad(source);

        // Assert
        assertNotEquals(source.getId(), result.getId(), "El ID no debe copiarse");
        assertNull(result.getId(), "El ID debe ser null para nueva entidad");
        assertEquals(source.getNombre(), result.getNombre());
        assertEquals(source.getApellidos(), result.getApellidos());
        assertEquals(source.getDni(), result.getDni());
        assertEquals(source.getEdad(), result.getEdad());
        assertEquals(source.getTipoTransporte(), result.getTipoTransporte());
        assertEquals(source.getPlaca(), result.getPlaca());
        assertEquals(source.getVehiculoInfo(), result.getVehiculoInfo());
        assertEquals(source.getCapacidad(), result.getCapacidad());
        assertEquals(source.getEstado(), result.getEstado());
    }

}