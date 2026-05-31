package com.example.backendastramaco;

import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.service.UsuarioService;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceUnitTest {

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
    private UsuarioService usuarioService;

    @Test
    @DisplayName("Debe crear usuario codificando password y activándolo por defecto")
    void crear_DebeCodificarPasswordYActivarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setUsername("admin");
        usuario.setPassword("123456");
        usuario.setRol(Rol.ADMIN);
        usuario.setActivo(false);

        when(passwordEncoder.encode("123456")).thenReturn("clave-codificada");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Usar any() en lugar de anyLong() porque el ID puede ser null antes de guardar
        doNothing().when(auditService).auditUsuario(any(), anyString(), any(), any(), any());

        Usuario resultado = usuarioService.crear(usuario);

        assertNotNull(resultado);
        assertEquals("admin", resultado.getUsername());
        assertEquals("clave-codificada", resultado.getPassword());
        assertEquals(Rol.ADMIN, resultado.getRol());
        assertEquals(Boolean.TRUE, resultado.getActivo());

        verify(passwordEncoder).encode("123456");
        verify(usuarioRepository).save(any(Usuario.class));
        verify(auditService, times(1)).auditUsuario(any(), eq("CREATE"), isNull(), any(), any());
    }

    @Test
    @DisplayName("Debe actualizar usuario correctamente")
    void actualizar_DebeActualizarUsuarioYAuditar() {
        // Arrange
        Long usuarioId = 1L;

        Usuario usuarioExistente = new Usuario();
        usuarioExistente.setId(usuarioId);
        usuarioExistente.setUsername("oldusername");
        usuarioExistente.setPassword("oldpassword");
        usuarioExistente.setRol(Rol.USER);
        usuarioExistente.setActivo(true);

        Usuario nuevosDatos = new Usuario();
        nuevosDatos.setUsername("newusername");
        nuevosDatos.setPassword("newpassword");
        nuevosDatos.setRol(Rol.ADMIN);
        nuevosDatos.setActivo(false);

        ArgumentCaptor<Usuario> oldCopyCaptor = ArgumentCaptor.forClass(Usuario.class);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioExistente));
        when(passwordEncoder.encode("newpassword")).thenReturn("newpassword-encoded");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditService).auditUsuario(anyLong(), anyString(), oldCopyCaptor.capture(), any(), any());

        // Act
        Usuario resultado = usuarioService.actualizar(usuarioId, nuevosDatos);

        // Assert
        assertNotNull(resultado);
        assertEquals("newusername", resultado.getUsername());
        assertEquals("newpassword-encoded", resultado.getPassword());
        assertEquals(Rol.ADMIN, resultado.getRol());
        assertFalse(resultado.getActivo());

        // Verificar que la copia no tiene ID
        Usuario oldCopy = oldCopyCaptor.getValue();
        assertNull(oldCopy.getId(), "La copia anterior no debe tener ID");

        verify(usuarioRepository).findById(usuarioId);
        verify(passwordEncoder).encode("newpassword");
        verify(usuarioRepository).save(usuarioExistente);
        verify(auditService, times(1)).auditUsuario(eq(usuarioId), eq("UPDATE"), any(), any(), any());
    }

    @Test
    @DisplayName("Debe actualizar usuario sin cambiar password cuando se envía null")
    void actualizar_DebeActualizarUsuarioSinCambiarPassword() {
        // Arrange
        Long usuarioId = 1L;

        Usuario usuarioExistente = new Usuario();
        usuarioExistente.setId(usuarioId);
        usuarioExistente.setUsername("oldusername");
        usuarioExistente.setPassword("existing-encoded-password");
        usuarioExistente.setRol(Rol.USER);
        usuarioExistente.setActivo(true);

        Usuario nuevosDatos = new Usuario();
        nuevosDatos.setUsername("newusername");
        nuevosDatos.setPassword(null);  // No cambiar password
        nuevosDatos.setRol(Rol.ADMIN);
        nuevosDatos.setActivo(false);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditService).auditUsuario(anyLong(), anyString(), any(), any(), any());

        // Act
        Usuario resultado = usuarioService.actualizar(usuarioId, nuevosDatos);

        // Assert
        assertNotNull(resultado);
        assertEquals("newusername", resultado.getUsername());
        assertEquals("existing-encoded-password", resultado.getPassword(), "La password no debe cambiar");
        assertEquals(Rol.ADMIN, resultado.getRol());
        assertFalse(resultado.getActivo());

        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository).save(usuarioExistente);
    }

    @Test
    @DisplayName("Debe eliminar usuario lógicamente (soft delete)")
    void eliminar_DebeEliminarUsuarioLogicamente() {
        // Arrange
        Long usuarioId = 1L;
        String username = "admin";

        Usuario usuarioExistente = new Usuario();
        usuarioExistente.setId(usuarioId);
        usuarioExistente.setUsername("testuser");
        usuarioExistente.setActivo(true);

        ArgumentCaptor<Usuario> oldCopyCaptor = ArgumentCaptor.forClass(Usuario.class);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditService).auditUsuario(anyLong(), anyString(), oldCopyCaptor.capture(), any(), any());

        // Act
        usuarioService.eliminar(usuarioId, username);

        // Assert
        assertNotNull(usuarioExistente.getDeletedAt(), "deletedAt debe estar presente");
        assertEquals(username, usuarioExistente.getDeletedBy(), "deletedBy debe ser el username");

        // Verificar que la copia no tiene ID
        Usuario oldCopy = oldCopyCaptor.getValue();
        assertNull(oldCopy.getId(), "La copia para auditoría no debe tener ID");

        verify(usuarioRepository).findById(usuarioId);
        verify(usuarioRepository).save(usuarioExistente);
        verify(auditService, times(1)).auditUsuario(eq(usuarioId), eq("DELETE"), any(), isNull(), any());
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar usuario que no existe")
    void actualizar_DebeLanzarExcepcion_CuandoUsuarioNoExiste() {
        // Arrange
        Long usuarioId = 999L;
        Usuario nuevosDatos = new Usuario();

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> usuarioService.actualizar(usuarioId, nuevosDatos));

        assertEquals("Usuario no encontrado", ex.getMessage());

        verify(usuarioRepository).findById(usuarioId);
        verify(usuarioRepository, never()).save(any());
        verify(auditService, never()).auditUsuario(anyLong(), anyString(), any(), any(), any());
    }


    @Test
    @DisplayName("Debe guardar usuario con password codificada y activo true")
    void crear_DebeGuardarUsuarioConValoresCorrectos() {
        Usuario usuario = new Usuario();
        usuario.setUsername("transportista1");
        usuario.setPassword("abc123");
        usuario.setRol(Rol.TRANSPORTISTA);

        when(passwordEncoder.encode("abc123")).thenReturn("abc123-codificada");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(auditService).auditUsuario(any(), anyString(), any(), any(), any());

        usuarioService.crear(usuario);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());

        Usuario usuarioGuardado = captor.getValue();

        assertEquals("transportista1", usuarioGuardado.getUsername());
        assertEquals("abc123-codificada", usuarioGuardado.getPassword());
        assertEquals(Rol.TRANSPORTISTA, usuarioGuardado.getRol());
        assertEquals(Boolean.TRUE, usuarioGuardado.getActivo());

        verify(auditService, times(1)).auditUsuario(any(), eq("CREATE"), isNull(), any(), any());
    }
}