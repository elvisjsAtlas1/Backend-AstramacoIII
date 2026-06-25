package com.example.backendastramaco.unitarias;

import com.example.backendastramaco.dto.TransportistaRequestDTO;
import com.example.backendastramaco.exception.DuplicateResourceException;
import com.example.backendastramaco.exception.ResourceNotFoundException;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.audit.AuditoriaTransportista;
import com.example.backendastramaco.model.enums.EstadoTransportista;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.repository.audit.AuditoriaTransportistaRepository;
import com.example.backendastramaco.service.TransportistaService;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransportistaServiceUnitTest {

    @Mock
    private TransportistaRepository transportistaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaTransportistaRepository auditoriaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private TransportistaService transportistaService;

    private Transportista transportista;
    private TransportistaRequestDTO requestDTO;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        // Configurar usuario base
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("juan.perez");
        usuario.setPassword("clave-codificada");
        usuario.setRol(Rol.TRANSPORTISTA);
        usuario.setActivo(true);

        // Configurar transportista base
        transportista = new Transportista();
        transportista.setId(1L);
        transportista.setUsuario(usuario);
        transportista.setNombre("Juan");
        transportista.setApellidos("Perez");
        transportista.setDni("12345678");
        transportista.setEdad(30);
        transportista.setTipoTransporte(TipoTransporte.CAMIONERO);
        transportista.setPlaca("ABC-123");
        transportista.setVehiculoInfo("Camion rojo");
        transportista.setCapacidad(10.5);
        transportista.setEstado(EstadoTransportista.ACTIVO);
        transportista.setCreatedAt(LocalDateTime.now());

        // Configurar DTO - SIN usuarioId
        requestDTO = new TransportistaRequestDTO();
        requestDTO.setNombre("Juan");
        requestDTO.setApellidos("Perez");
        requestDTO.setDni("12345678");
        requestDTO.setEdad(30);
        requestDTO.setTipoTransporte(TipoTransporte.CAMIONERO);
        requestDTO.setPlaca("ABC-123");
        requestDTO.setVehiculoInfo("Camion rojo");
        requestDTO.setCapacidad(10.5);

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

    // ========== PRUEBAS DE CREACIÓN ==========

    @Test
    @DisplayName("Debe crear transportista con usuario automático y estado ACTIVO por defecto")
    void crear_DebeCrearTransportistaConEstadoPorDefecto() {
        // Arrange
        TransportistaRequestDTO dto = new TransportistaRequestDTO();
        dto.setNombre("Juan");
        dto.setApellidos("Perez");
        dto.setDni("12345678");
        dto.setEdad(30);
        dto.setTipoTransporte(TipoTransporte.CAMIONERO);
        dto.setPlaca("ABC-123");
        dto.setVehiculoInfo("Camion rojo");
        dto.setCapacidad(10.5);

        // ✅ Mock para generar username único
        when(usuarioRepository.findByUsername("juan.perez")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("12345678")).thenReturn("clave-codificada");

        // ✅ Mock para guardar usuario
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        // ✅ Mock para validar DNI y placa
        when(transportistaRepository.existsByDni("12345678")).thenReturn(false);
        when(transportistaRepository.existsByPlaca("ABC-123")).thenReturn(false);

        // ✅ Mock para guardar transportista
        when(transportistaRepository.save(any(Transportista.class)))
                .thenAnswer(invocation -> {
                    Transportista t = invocation.getArgument(0);
                    t.setId(1L);
                    return t;
                });
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        Transportista resultado = transportistaService.crear(dto);

        // Assert
        assertNotNull(resultado);
        assertNotNull(resultado.getId());
        assertEquals(1L, resultado.getId());
        assertEquals(EstadoTransportista.ACTIVO, resultado.getEstado());
        assertNotNull(resultado.getUsuario());
        assertEquals("juan.perez", resultado.getUsuario().getUsername());
        assertEquals(Rol.TRANSPORTISTA, resultado.getUsuario().getRol());

        verify(transportistaRepository).save(any(Transportista.class));
        verify(usuarioRepository).save(any(Usuario.class));

        // Verificar auditoría
        ArgumentCaptor<AuditoriaTransportista> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaTransportista.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaTransportista auditoria = auditoriaCaptor.getValue();
        assertEquals("CREATE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
        assertEquals(1L, auditoria.getTransportistaId());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el DNI ya existe")
    void crear_DebeLanzarExcepcion_CuandoDNIYaExiste() {
        // Arrange
        when(transportistaRepository.existsByDni("12345678")).thenReturn(true);

        // Act & Assert
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> transportistaService.crear(requestDTO));

        assertEquals("El DNI '12345678' ya está registrado", ex.getMessage());
        verify(transportistaRepository, never()).save(any(Transportista.class));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la placa ya existe")
    void crear_DebeLanzarExcepcion_CuandoPlacaYaExiste() {
        // Arrange
        when(transportistaRepository.existsByDni("12345678")).thenReturn(false);
        when(transportistaRepository.existsByPlaca("ABC-123")).thenReturn(true);

        // Act & Assert
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> transportistaService.crear(requestDTO));

        assertEquals("La placa 'ABC-123' ya está registrada", ex.getMessage());
        verify(transportistaRepository, never()).save(any(Transportista.class));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe generar username único cuando el username base ya existe")
    void crear_DebeGenerarUsernameUnico() {
        // Arrange
        TransportistaRequestDTO dto = new TransportistaRequestDTO();
        dto.setNombre("Juan");
        dto.setApellidos("Perez");
        dto.setDni("12345678");
        dto.setEdad(30);
        dto.setTipoTransporte(TipoTransporte.CAMIONERO);
        dto.setPlaca("ABC-123");
        dto.setVehiculoInfo("Camion");
        dto.setCapacidad(8.0);

        // ✅ Mock para username ya existe
        when(usuarioRepository.findByUsername("juan.perez"))
                .thenReturn(Optional.of(new Usuario()));
        when(usuarioRepository.findByUsername("juan.perez1"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("12345678")).thenReturn("clave-codificada");

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        when(transportistaRepository.existsByDni("12345678")).thenReturn(false);
        when(transportistaRepository.existsByPlaca("ABC-123")).thenReturn(false);

        when(transportistaRepository.save(any(Transportista.class)))
                .thenAnswer(invocation -> {
                    Transportista t = invocation.getArgument(0);
                    t.setId(1L);
                    return t;
                });
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        Transportista resultado = transportistaService.crear(dto);

        // Assert
        assertNotNull(resultado);
        assertNotNull(resultado.getUsuario());
        assertEquals("juan.perez1", resultado.getUsuario().getUsername());

        verify(usuarioRepository, atLeast(2)).findByUsername(anyString());
        verify(usuarioRepository).save(any(Usuario.class));
        verify(transportistaRepository).save(any(Transportista.class));
    }

    @Test
    @DisplayName("Debe asignar estado ACTIVO por defecto en creación")
    void crear_DebeAsignarEstadoActivoPorDefecto() {
        // Arrange
        TransportistaRequestDTO dto = new TransportistaRequestDTO();
        dto.setNombre("Ana");
        dto.setApellidos("Lopez");
        dto.setDni("87654321");
        dto.setEdad(28);
        dto.setTipoTransporte(TipoTransporte.VOLQUETERO);
        dto.setPlaca("XYZ-999");
        dto.setVehiculoInfo("Unidad azul");
        dto.setCapacidad(15.0);

        when(usuarioRepository.findByUsername("ana.lopez")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("87654321")).thenReturn("clave-codificada");

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        when(transportistaRepository.existsByDni("87654321")).thenReturn(false);
        when(transportistaRepository.existsByPlaca("XYZ-999")).thenReturn(false);

        when(transportistaRepository.save(any(Transportista.class)))
                .thenAnswer(invocation -> {
                    Transportista t = invocation.getArgument(0);
                    t.setId(1L);
                    return t;
                });
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        Transportista resultado = transportistaService.crear(dto);

        // Assert
        assertNotNull(resultado);
        assertEquals(EstadoTransportista.ACTIVO, resultado.getEstado());
    }

    // ========== PRUEBAS DE LISTAR ==========

    @Test
    @DisplayName("Debe listar transportistas con paginación")
    void listar_DebeRetornarPaginaDeTransportistas() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transportista> transportistaPage = new PageImpl<>(List.of(transportista));
        when(transportistaRepository.findByDeletedAtIsNull(pageable)).thenReturn(transportistaPage);

        // Act
        Page<Transportista> resultado = transportistaService.listar(pageable, null, null);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertEquals("Juan", resultado.getContent().get(0).getNombre());

        verify(transportistaRepository).findByDeletedAtIsNull(pageable);
    }

    @Test
    @DisplayName("Debe listar transportistas con filtro por tipo")
    void listar_DebeFiltrarPorTipo() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transportista> transportistaPage = new PageImpl<>(List.of(transportista));
        when(transportistaRepository.findByTipoTransporteAndDeletedAtIsNull(
                TipoTransporte.CAMIONERO, pageable)).thenReturn(transportistaPage);

        // Act
        Page<Transportista> resultado = transportistaService.listar(pageable, "CAMIONERO", null);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        verify(transportistaRepository).findByTipoTransporteAndDeletedAtIsNull(TipoTransporte.CAMIONERO, pageable);
    }

    @Test
    @DisplayName("Debe listar transportistas con filtro por estado")
    void listar_DebeFiltrarPorEstado() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transportista> transportistaPage = new PageImpl<>(List.of(transportista));
        when(transportistaRepository.findByEstadoAndDeletedAtIsNull(
                EstadoTransportista.ACTIVO, pageable)).thenReturn(transportistaPage);

        // Act
        Page<Transportista> resultado = transportistaService.listar(pageable, null, "ACTIVO");

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        verify(transportistaRepository).findByEstadoAndDeletedAtIsNull(EstadoTransportista.ACTIVO, pageable);
    }

    @Test
    @DisplayName("Debe listar transportistas activos por tipo")
    void listarPorTipo_DebeRetornarTransportistasActivos() {
        // Arrange
        when(transportistaRepository.findByTipoTransporteAndEstadoAndDeletedAtIsNull(
                TipoTransporte.CAMIONERO,
                EstadoTransportista.ACTIVO
        )).thenReturn(List.of(transportista, new Transportista()));

        // Act
        var resultado = transportistaService.listarPorTipo(TipoTransporte.CAMIONERO);

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
    }

    @Test
    @DisplayName("Debe listar todos los transportistas (incluyendo eliminados)")
    void listarTodos_DebeRetornarTodos() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transportista> transportistaPage = new PageImpl<>(List.of(transportista, new Transportista()));
        when(transportistaRepository.findAll(pageable)).thenReturn(transportistaPage);

        // Act
        Page<Transportista> resultado = transportistaService.listarTodos(pageable);

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.getContent().size());
        verify(transportistaRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Debe listar transportistas eliminados")
    void listarEliminados_DebeRetornarEliminados() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        transportista.setDeletedAt(LocalDateTime.now());
        Page<Transportista> transportistaPage = new PageImpl<>(List.of(transportista));
        when(transportistaRepository.findByDeletedAtIsNotNull(pageable)).thenReturn(transportistaPage);

        // Act
        Page<Transportista> resultado = transportistaService.listarEliminados(pageable);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertNotNull(resultado.getContent().get(0).getDeletedAt());
        verify(transportistaRepository).findByDeletedAtIsNotNull(pageable);
    }

    // ========== PRUEBAS DE OBTENER ==========

    @Test
    @DisplayName("Debe obtener transportista por ID")
    void obtenerPorId_DebeRetornarTransportista_CuandoExiste() {
        // Arrange
        Long transportistaId = 1L;
        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.of(transportista));

        // Act
        Transportista resultado = transportistaService.obtenerPorId(transportistaId);

        // Assert
        assertNotNull(resultado);
        assertEquals(transportistaId, resultado.getId());
        assertEquals("Juan", resultado.getNombre());

        verify(transportistaRepository).findById(transportistaId);
    }

    @Test
    @DisplayName("Debe lanzar excepción al obtener transportista por ID que no existe")
    void obtenerPorId_DebeLanzarExcepcion_CuandoNoExiste() {
        // Arrange
        Long transportistaId = 999L;
        when(transportistaRepository.findById(transportistaId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> transportistaService.obtenerPorId(transportistaId));

        assertEquals("Transportista no encontrado con ID: 999", ex.getMessage());
    }

    @Test
    @DisplayName("Debe obtener transportista por DNI")
    void obtenerPorDni_DebeRetornarTransportista_CuandoExiste() {
        // Arrange
        String dni = "12345678";
        when(transportistaRepository.findByDniAndDeletedAtIsNull(dni))
                .thenReturn(Optional.of(transportista));

        // Act
        Transportista resultado = transportistaService.obtenerPorDni(dni);

        // Assert
        assertNotNull(resultado);
        assertEquals(dni, resultado.getDni());

        verify(transportistaRepository).findByDniAndDeletedAtIsNull(dni);
    }

    @Test
    @DisplayName("Debe obtener transportista por usuario ID")
    void obtenerPorUsuarioId_DebeRetornarTransportista_CuandoExiste() {
        // Arrange
        Long usuarioId = 1L;
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(transportistaRepository.findByUsuarioAndDeletedAtIsNull(usuario))
                .thenReturn(Optional.of(transportista));

        // Act
        Transportista resultado = transportistaService.obtenerPorUsuarioId(usuarioId);

        // Assert
        assertNotNull(resultado);
        assertEquals(usuarioId, resultado.getUsuario().getId());

        verify(usuarioRepository).findById(usuarioId);
        verify(transportistaRepository).findByUsuarioAndDeletedAtIsNull(usuario);
    }

    // ========== PRUEBAS PARA ACTUALIZAR ==========

    @Test
    @DisplayName("Debe actualizar transportista exitosamente")
    void actualizar_DebeActualizarTransportista_CuandoExiste() {
        // Arrange
        Long transportistaId = 1L;

        TransportistaRequestDTO updateDTO = new TransportistaRequestDTO();
        updateDTO.setNombre("Juan Carlos");
        updateDTO.setApellidos("Perez Gomez");
        updateDTO.setDni("87654321");
        updateDTO.setEdad(35);
        updateDTO.setTipoTransporte(TipoTransporte.VOLQUETERO);
        updateDTO.setPlaca("XYZ-789");
        updateDTO.setVehiculoInfo("Camion azul");
        updateDTO.setCapacidad(15.0);

        when(transportistaRepository.findById(transportistaId))
                .thenReturn(Optional.of(transportista));
        when(transportistaRepository.existsByDni("87654321")).thenReturn(false);
        when(transportistaRepository.existsByPlaca("XYZ-789")).thenReturn(false);
        when(transportistaRepository.save(any(Transportista.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        Transportista resultado = transportistaService.actualizar(transportistaId, updateDTO);

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

        verify(transportistaRepository).findById(transportistaId);
        verify(transportistaRepository).save(any(Transportista.class));

        // Verificar auditoría
        ArgumentCaptor<AuditoriaTransportista> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaTransportista.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaTransportista auditoria = auditoriaCaptor.getValue();
        assertEquals("UPDATE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
        assertEquals("Juan", auditoria.getNombreAnterior());
        assertEquals("Juan Carlos", auditoria.getNombreNuevo());
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar con DNI duplicado")
    void actualizar_DebeLanzarExcepcion_CuandoDNIDuplicado() {
        // Arrange
        Long transportistaId = 1L;

        TransportistaRequestDTO updateDTO = new TransportistaRequestDTO();
        updateDTO.setNombre("Juan Carlos");
        updateDTO.setApellidos("Perez Gomez");
        updateDTO.setDni("87654321");
        updateDTO.setEdad(35);
        updateDTO.setTipoTransporte(TipoTransporte.VOLQUETERO);
        updateDTO.setPlaca("XYZ-789");
        updateDTO.setVehiculoInfo("Camion azul");
        updateDTO.setCapacidad(15.0);

        when(transportistaRepository.findById(transportistaId))
                .thenReturn(Optional.of(transportista));
        when(transportistaRepository.existsByDni("87654321")).thenReturn(true);

        // Act & Assert
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> transportistaService.actualizar(transportistaId, updateDTO));

        assertEquals("El DNI '87654321' ya está registrado", ex.getMessage());
        verify(transportistaRepository, never()).save(any(Transportista.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar con placa duplicada")
    void actualizar_DebeLanzarExcepcion_CuandoPlacaDuplicada() {
        // Arrange
        Long transportistaId = 1L;

        TransportistaRequestDTO updateDTO = new TransportistaRequestDTO();
        updateDTO.setNombre("Juan Carlos");
        updateDTO.setApellidos("Perez Gomez");
        updateDTO.setDni("87654321");
        updateDTO.setEdad(35);
        updateDTO.setTipoTransporte(TipoTransporte.VOLQUETERO);
        updateDTO.setPlaca("XYZ-789");
        updateDTO.setVehiculoInfo("Camion azul");
        updateDTO.setCapacidad(15.0);

        when(transportistaRepository.findById(transportistaId))
                .thenReturn(Optional.of(transportista));
        when(transportistaRepository.existsByDni("87654321")).thenReturn(false);
        when(transportistaRepository.existsByPlaca("XYZ-789")).thenReturn(true);

        // Act & Assert
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> transportistaService.actualizar(transportistaId, updateDTO));

        assertEquals("La placa 'XYZ-789' ya está registrada", ex.getMessage());
        verify(transportistaRepository, never()).save(any(Transportista.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar transportista que no existe")
    void actualizar_DebeLanzarExcepcion_CuandoTransportistaNoExiste() {
        // Arrange
        Long transportistaId = 999L;
        TransportistaRequestDTO updateDTO = new TransportistaRequestDTO();
        updateDTO.setNombre("Carlos");
        updateDTO.setApellidos("Gomez");
        updateDTO.setDni("11111111");
        updateDTO.setEdad(25);
        updateDTO.setTipoTransporte(TipoTransporte.VOLQUETERO);
        updateDTO.setPlaca("ABC-111");
        updateDTO.setVehiculoInfo("Test");
        updateDTO.setCapacidad(5.0);

        when(transportistaRepository.findById(transportistaId))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> transportistaService.actualizar(transportistaId, updateDTO)
        );

        assertEquals("Transportista no encontrado con ID: 999", exception.getMessage());
        verify(transportistaRepository, never()).save(any(Transportista.class));
    }

    // ========== PRUEBAS PARA ELIMINAR ==========

    @Test
    @DisplayName("Debe eliminar transportista lógicamente (soft delete)")
    void eliminar_DebeEliminarTransportistaLogicamente_CuandoExiste() {
        // Arrange
        Long transportistaId = 1L;
        String username = "admin";

        when(transportistaRepository.findById(transportistaId))
                .thenReturn(Optional.of(transportista));
        when(transportistaRepository.save(any(Transportista.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        transportistaService.eliminar(transportistaId, username);

        // Assert
        assertEquals(EstadoTransportista.INACTIVO, transportista.getEstado());
        assertNotNull(transportista.getDeletedAt());
        assertEquals(username, transportista.getDeletedBy());

        verify(transportistaRepository).findById(transportistaId);
        verify(transportistaRepository).save(transportista);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaTransportista> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaTransportista.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaTransportista auditoria = auditoriaCaptor.getValue();
        assertEquals("DELETE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
        assertEquals("Juan", auditoria.getNombreAnterior());
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar transportista que ya está eliminado")
    void eliminar_DebeLanzarExcepcion_CuandoTransportistaYaEliminado() {
        // Arrange
        Long transportistaId = 1L;
        transportista.setDeletedAt(LocalDateTime.now());
        transportista.setDeletedBy("admin");

        when(transportistaRepository.findById(transportistaId))
                .thenReturn(Optional.of(transportista));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transportistaService.eliminar(transportistaId, "admin"));

        assertEquals("El transportista ya está eliminado", ex.getMessage());
        verify(transportistaRepository, never()).save(any(Transportista.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar transportista que no existe")
    void eliminar_DebeLanzarExcepcion_CuandoTransportistaNoExiste() {
        // Arrange
        Long transportistaId = 999L;

        when(transportistaRepository.findById(transportistaId))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> transportistaService.eliminar(transportistaId, "admin"));

        assertEquals("Transportista no encontrado con ID: 999", ex.getMessage());
        verify(transportistaRepository, never()).save(any());
    }

    // ========== PRUEBAS PARA RESTAURAR ==========

    @Test
    @DisplayName("Debe restaurar transportista eliminado lógicamente")
    void restaurar_DebeRestaurarTransportistaEliminado() {
        // Arrange
        Long transportistaId = 1L;
        transportista.setDeletedAt(LocalDateTime.now());
        transportista.setDeletedBy("admin");
        transportista.setEstado(EstadoTransportista.INACTIVO);

        when(transportistaRepository.findById(transportistaId))
                .thenReturn(Optional.of(transportista));
        when(transportistaRepository.save(any(Transportista.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        transportistaService.restaurar(transportistaId);

        // Assert
        assertNull(transportista.getDeletedAt());
        assertNull(transportista.getDeletedBy());
        assertEquals(EstadoTransportista.ACTIVO, transportista.getEstado());

        verify(transportistaRepository).save(transportista);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaTransportista> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaTransportista.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaTransportista auditoria = auditoriaCaptor.getValue();
        assertEquals("RESTORE", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
    }

    @Test
    @DisplayName("Debe lanzar excepción al restaurar transportista no eliminado")
    void restaurar_DebeLanzarExcepcion_CuandoTransportistaNoEliminado() {
        // Arrange
        Long transportistaId = 1L;
        transportista.setDeletedAt(null);

        when(transportistaRepository.findById(transportistaId))
                .thenReturn(Optional.of(transportista));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transportistaService.restaurar(transportistaId));

        assertEquals("El transportista no está eliminado", ex.getMessage());
        verify(transportistaRepository, never()).save(any(Transportista.class));
    }

    // ========== PRUEBAS PARA CAMBIAR ESTADO ==========

    @Test
    @DisplayName("Debe cambiar estado del transportista")
    void cambiarEstado_DebeCambiarEstadoTransportista() {
        // Arrange
        Long transportistaId = 1L;
        String nuevoEstado = "INACTIVO";

        when(transportistaRepository.findById(transportistaId))
                .thenReturn(Optional.of(transportista));
        when(transportistaRepository.save(any(Transportista.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        transportistaService.cambiarEstado(transportistaId, nuevoEstado);

        // Assert
        assertEquals(EstadoTransportista.INACTIVO, transportista.getEstado());
        verify(transportistaRepository).save(transportista);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaTransportista> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaTransportista.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaTransportista auditoria = auditoriaCaptor.getValue();
        assertEquals("UPDATE_ESTADO", auditoria.getAccion());
        assertEquals("ACTIVO", auditoria.getEstadoAnterior());
        assertEquals("INACTIVO", auditoria.getEstadoNuevo());
    }

    // ========== PRUEBAS PARA ELIMINAR PERMANENTE ==========

    @Test
    @DisplayName("Debe eliminar permanentemente un transportista")
    void eliminarPermanente_DebeEliminarTransportistaFisicamente() {
        // Arrange
        Long transportistaId = 1L;

        when(transportistaRepository.findById(transportistaId))
                .thenReturn(Optional.of(transportista));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        transportistaService.eliminarPermanente(transportistaId);

        // Assert
        verify(transportistaRepository).delete(transportista);

        // Verificar auditoría
        ArgumentCaptor<AuditoriaTransportista> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaTransportista.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaTransportista auditoria = auditoriaCaptor.getValue();
        assertEquals("DELETE_PERMANENT", auditoria.getAccion());
        assertEquals("admin", auditoria.getUsername());
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar permanentemente transportista que no existe")
    void eliminarPermanente_DebeLanzarExcepcion_CuandoNoExiste() {
        // Arrange
        Long transportistaId = 999L;

        when(transportistaRepository.findById(transportistaId))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> transportistaService.eliminarPermanente(transportistaId));

        assertEquals("Transportista no encontrado con ID: 999", ex.getMessage());
        verify(transportistaRepository, never()).delete(any());
    }
}