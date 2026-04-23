package com.example.backendastramaco;

import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.service.UsuarioService;
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
class UsuarioServiceTests {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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

        Usuario resultado = usuarioService.crear(usuario);

        assertNotNull(resultado);
        assertEquals("admin", resultado.getUsername());
        assertEquals("clave-codificada", resultado.getPassword());
        assertEquals(Rol.ADMIN, resultado.getRol());
        assertTrue(Boolean.TRUE.equals(resultado.getActivo()));

        verify(passwordEncoder).encode("123456");
        verify(usuarioRepository).save(any(Usuario.class));
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

        usuarioService.crear(usuario);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());

        Usuario usuarioGuardado = captor.getValue();

        assertEquals("transportista1", usuarioGuardado.getUsername());
        assertEquals("abc123-codificada", usuarioGuardado.getPassword());
        assertEquals(Rol.TRANSPORTISTA, usuarioGuardado.getRol());
        assertTrue(Boolean.TRUE.equals(usuarioGuardado.getActivo()));
    }
}