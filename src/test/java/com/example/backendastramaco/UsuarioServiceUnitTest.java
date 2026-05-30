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