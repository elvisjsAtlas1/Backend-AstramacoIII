package com.example.backendastramaco.unitarias;

import com.example.backendastramaco.dto.UsuarioRequestDTO;
import com.example.backendastramaco.exception.DuplicateResourceException;
import com.example.backendastramaco.exception.ResourceNotFoundException;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.audit.AuditoriaUsuario;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.repository.audit.AuditoriaUsuarioRepository;
import com.example.backendastramaco.service.UsuarioService;
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
class UsuarioServiceUnitTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaUsuarioRepository auditoriaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private UsuarioRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        // Configurar usuario base
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("testuser");
        usuario.setPassword("encodedPassword");
        usuario.setRol(Rol.ADMIN);
        usuario.setActivo(true);
        usuario.setCreatedAt(LocalDateTime.now());

        // Configurar DTO
        requestDTO = new UsuarioRequestDTO();
        requestDTO.setUsername("testuser");
        requestDTO.setPassword("testpass");
        requestDTO.setRol(Rol.ADMIN);

        // NO hacer stubs aquí - se harán en cada prueba específica
        // ✅ ELIMINAR stubs innecesarios
    }

    @Test
    @DisplayName("Debe crear usuario codificando password y activarlo por defecto")
    void crear_DebeCodificarPasswordYActivarUsuario() {
        // Arrange
        // ✅ Configurar SecurityContext para esta prueba
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        when(usuarioRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("testpass")).thenReturn("clave-codificada");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        Usuario resultado = usuarioService.crear(requestDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("testuser", resultado.getUsername());
        assertEquals("clave-codificada", resultado.getPassword());
        assertEquals(Rol.ADMIN, resultado.getRol());
        assertTrue(resultado.getActivo());

        verify(passwordEncoder).encode("testpass");
        verify(usuarioRepository).save(any(Usuario.class));

        // Verificar que se guardó auditoría
        ArgumentCaptor<AuditoriaUsuario> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaUsuario.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaUsuario auditoria = auditoriaCaptor.getValue();
        assertEquals("CREATE", auditoria.getAccion());
        assertEquals("testuser", auditoria.getUsername()); // ✅ Ahora es "testuser"
        assertEquals(1L, auditoria.getUsuarioId());
    }

    @Test
    @DisplayName("Debe lanzar excepción al crear usuario con username duplicado")
    void crear_DebeLanzarExcepcion_CuandoUsernameDuplicado() {
        // Arrange
        when(usuarioRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> usuarioService.crear(requestDTO));

        assertEquals("El username 'testuser' ya está registrado", ex.getMessage());

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(auditoriaRepository, never()).save(any(AuditoriaUsuario.class));
    }

    @Test
    @DisplayName("Debe actualizar usuario correctamente")
    void actualizar_DebeActualizarUsuarioYAuditar() {
        // Arrange
        Long usuarioId = 1L;

        UsuarioRequestDTO updateDTO = new UsuarioRequestDTO();
        updateDTO.setUsername("newusername");
        updateDTO.setPassword("newpassword");
        updateDTO.setRol(Rol.USER);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByUsername("newusername")).thenReturn(false);
        when(passwordEncoder.encode("newpassword")).thenReturn("newpassword-encoded");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        Usuario resultado = usuarioService.actualizar(usuarioId, updateDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals("newusername", resultado.getUsername());
        assertEquals("newpassword-encoded", resultado.getPassword());
        assertEquals(Rol.USER, resultado.getRol());

        verify(usuarioRepository).findById(usuarioId);
        verify(passwordEncoder).encode("newpassword");
        verify(usuarioRepository).save(usuario);

        // Verificar auditoría de actualización
        ArgumentCaptor<AuditoriaUsuario> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaUsuario.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaUsuario auditoria = auditoriaCaptor.getValue();
        assertEquals("UPDATE", auditoria.getAccion());
        assertEquals("testuser", auditoria.getUsernameAnterior());
        assertEquals("newusername", auditoria.getUsernameNuevo());
    }

    @Test
    @DisplayName("Debe actualizar usuario sin cambiar password cuando se envía null")
    void actualizar_DebeActualizarUsuarioSinCambiarPassword() {
        // Arrange
        Long usuarioId = 1L;

        UsuarioRequestDTO updateDTO = new UsuarioRequestDTO();
        updateDTO.setUsername("newusername");
        updateDTO.setPassword(null);  // No cambiar password
        updateDTO.setRol(Rol.USER);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByUsername("newusername")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        Usuario resultado = usuarioService.actualizar(usuarioId, updateDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals("newusername", resultado.getUsername());
        assertEquals("encodedPassword", resultado.getPassword(), "La password no debe cambiar");
        assertEquals(Rol.USER, resultado.getRol());

        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar username con duplicado")
    void actualizar_DebeLanzarExcepcion_CuandoUsernameDuplicado() {
        // Arrange
        Long usuarioId = 1L;

        UsuarioRequestDTO updateDTO = new UsuarioRequestDTO();
        updateDTO.setUsername("existinguser");
        updateDTO.setPassword("password");
        updateDTO.setRol(Rol.USER);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> usuarioService.actualizar(usuarioId, updateDTO));

        assertEquals("El username 'existinguser' ya está registrado", ex.getMessage());

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar usuario que no existe")
    void actualizar_DebeLanzarExcepcion_CuandoUsuarioNoExiste() {
        // Arrange
        Long usuarioId = 999L;
        UsuarioRequestDTO updateDTO = new UsuarioRequestDTO();

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> usuarioService.actualizar(usuarioId, updateDTO));

        assertEquals("Usuario no encontrado con ID: 999", ex.getMessage());

        verify(usuarioRepository).findById(usuarioId);
        verify(usuarioRepository, never()).save(any());
        verify(auditoriaRepository, never()).save(any(AuditoriaUsuario.class));
    }

    @Test
    @DisplayName("Debe eliminar usuario lógicamente (soft delete)")
    void eliminar_DebeEliminarUsuarioLogicamente() {
        // Arrange
        Long usuarioId = 1L;

        // ✅ Configurar SecurityContext para esta prueba
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        usuarioService.eliminar(usuarioId);

        // Assert
        assertNotNull(usuario.getDeletedAt(), "deletedAt debe estar presente");
        assertEquals("testuser", usuario.getDeletedBy(), "deletedBy debe ser el usuario actual");

        verify(usuarioRepository).findById(usuarioId);
        verify(usuarioRepository).save(usuario);

        // Verificar auditoría de eliminación
        ArgumentCaptor<AuditoriaUsuario> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaUsuario.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaUsuario auditoria = auditoriaCaptor.getValue();
        assertEquals("DELETE", auditoria.getAccion());
        assertEquals("testuser", auditoria.getUsername()); // ✅ Ahora es "testuser"
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar usuario que ya está eliminado")
    void eliminar_DebeLanzarExcepcion_CuandoUsuarioYaEliminado() {
        // Arrange
        Long usuarioId = 1L;
        usuario.setDeletedAt(LocalDateTime.now());
        usuario.setDeletedBy("admin");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> usuarioService.eliminar(usuarioId));

        assertEquals("El usuario ya está eliminado", ex.getMessage());

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe restaurar usuario eliminado lógicamente")
    void restaurar_DebeRestaurarUsuarioEliminado() {
        // Arrange
        Long usuarioId = 1L;
        usuario.setDeletedAt(LocalDateTime.now());
        usuario.setDeletedBy("admin");

        // ✅ Configurar SecurityContext para esta prueba
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        usuarioService.restaurar(usuarioId);

        // Assert
        assertNull(usuario.getDeletedAt(), "deletedAt debe ser null");
        assertNull(usuario.getDeletedBy(), "deletedBy debe ser null");

        verify(usuarioRepository).save(usuario);

        // Verificar auditoría de restauración
        ArgumentCaptor<AuditoriaUsuario> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaUsuario.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaUsuario auditoria = auditoriaCaptor.getValue();
        assertEquals("RESTORE", auditoria.getAccion());
        assertEquals("testuser", auditoria.getUsername()); // ✅ Ahora es "testuser"
    }

    @Test
    @DisplayName("Debe lanzar excepción al restaurar usuario no eliminado")
    void restaurar_DebeLanzarExcepcion_CuandoUsuarioNoEliminado() {
        // Arrange
        Long usuarioId = 1L;
        usuario.setDeletedAt(null);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> usuarioService.restaurar(usuarioId));

        assertEquals("El usuario no está eliminado", ex.getMessage());

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe listar usuarios activos con paginación")
    void listar_DebeRetornarPaginaDeUsuariosActivos() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Usuario> usuarioPage = new PageImpl<>(List.of(usuario));
        when(usuarioRepository.findByDeletedAtIsNull(pageable)).thenReturn(usuarioPage);

        // Act
        Page<Usuario> result = usuarioService.listar(pageable, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("testuser", result.getContent().get(0).getUsername());

        verify(usuarioRepository).findByDeletedAtIsNull(pageable);
    }

    @Test
    @DisplayName("Debe listar usuarios con filtro por rol")
    void listar_DebeFiltrarPorRol() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Usuario> usuarioPage = new PageImpl<>(List.of(usuario));
        when(usuarioRepository.findByRolAndDeletedAtIsNull("ADMIN", pageable)).thenReturn(usuarioPage);

        // Act
        Page<Usuario> result = usuarioService.listar(pageable, "ADMIN", null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(usuarioRepository).findByRolAndDeletedAtIsNull("ADMIN", pageable);
    }

    @Test
    @DisplayName("Debe listar usuarios eliminados")
    void listarEliminados_DebeRetornarUsuariosEliminados() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        usuario.setDeletedAt(LocalDateTime.now());
        Page<Usuario> usuarioPage = new PageImpl<>(List.of(usuario));
        when(usuarioRepository.findByDeletedAtIsNotNull(pageable)).thenReturn(usuarioPage);

        // Act
        Page<Usuario> result = usuarioService.listarEliminados(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertNotNull(result.getContent().get(0).getDeletedAt());

        verify(usuarioRepository).findByDeletedAtIsNotNull(pageable);
    }

    @Test
    @DisplayName("Debe obtener usuario por ID")
    void obtenerPorId_DebeRetornarUsuario_CuandoExiste() {
        // Arrange
        Long usuarioId = 1L;
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        // Act
        Usuario result = usuarioService.obtenerPorId(usuarioId);

        // Assert
        assertNotNull(result);
        assertEquals(usuarioId, result.getId());
        assertEquals("testuser", result.getUsername());

        verify(usuarioRepository).findById(usuarioId);
    }

    @Test
    @DisplayName("Debe lanzar excepción al obtener usuario por ID que no existe")
    void obtenerPorId_DebeLanzarExcepcion_CuandoNoExiste() {
        // Arrange
        Long usuarioId = 999L;
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> usuarioService.obtenerPorId(usuarioId));

        assertEquals("Usuario no encontrado con ID: 999", ex.getMessage());
    }

    @Test
    @DisplayName("Debe obtener usuario por username")
    void obtenerPorUsername_DebeRetornarUsuario_CuandoExiste() {
        // Arrange
        String username = "testuser";
        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(usuario));

        // Act
        Usuario result = usuarioService.obtenerPorUsername(username);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());

        verify(usuarioRepository).findByUsername(username);
    }

    @Test
    @DisplayName("Debe cambiar estado del usuario")
    void cambiarEstado_DebeCambiarEstadoUsuario() {
        // Arrange
        Long usuarioId = 1L;
        Boolean nuevoEstado = false;

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        usuarioService.cambiarEstado(usuarioId, nuevoEstado);

        // Assert
        assertFalse(usuario.getActivo());
        verify(usuarioRepository).save(usuario);

        // Verificar auditoría de cambio de estado
        ArgumentCaptor<AuditoriaUsuario> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaUsuario.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaUsuario auditoria = auditoriaCaptor.getValue();
        assertEquals("UPDATE_ESTADO", auditoria.getAccion());
    }

    @Test
    @DisplayName("Debe eliminar permanentemente un usuario")
    void eliminarPermanente_DebeEliminarUsuarioFisicamente() {
        // Arrange
        Long usuarioId = 1L;
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        usuarioService.eliminarPermanente(usuarioId);

        // Assert
        verify(usuarioRepository).delete(usuario);

        // Verificar auditoría de eliminación permanente
        ArgumentCaptor<AuditoriaUsuario> auditoriaCaptor = ArgumentCaptor.forClass(AuditoriaUsuario.class);
        verify(auditoriaRepository).save(auditoriaCaptor.capture());

        AuditoriaUsuario auditoria = auditoriaCaptor.getValue();
        assertEquals("DELETE_PERMANENT", auditoria.getAccion());
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar permanentemente usuario que no existe")
    void eliminarPermanente_DebeLanzarExcepcion_CuandoNoExiste() {
        // Arrange
        Long usuarioId = 999L;
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> usuarioService.eliminarPermanente(usuarioId));

        assertEquals("Usuario no encontrado con ID: 999", ex.getMessage());
        verify(usuarioRepository, never()).delete(any());
    }
}