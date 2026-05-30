package com.example.backendastramaco.controller;

import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.security.dto.AuthRequest;
import com.example.backendastramaco.security.dto.AuthResponse;
import com.example.backendastramaco.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuthControllerUnitTest {

    private AuthController authController;
    private AuthenticationManager authManager;
    private JwtUtil jwtUtil;
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        authManager = Mockito.mock(AuthenticationManager.class);
        jwtUtil = Mockito.mock(JwtUtil.class);
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        // Instanciamos el controlador directamente pasándole los mocks por constructor
        authController = new AuthController(authManager, jwtUtil, usuarioRepository);
    }

    @Test
    @DisplayName("Debe ejecutar el cuerpo interno de login para cobertura de JaCoCo sin levantar Spring")
    void login_DebeEjecutarMetodoInternoCompletamente() {
        // Arrange
        AuthRequest request = new AuthRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        Usuario usuarioSimulado = new Usuario();
        usuarioSimulado.setUsername("admin");
        usuarioSimulado.setRol(Rol.ADMIN);

        when(jwtUtil.generateToken("admin")).thenReturn("mocked-jwt-token-2026");
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioSimulado));

        // Act
        AuthResponse response = authController.login(request);

        // Assert - Validamos los datos para que JaCoCo marque las líneas como cubiertas
        assertNotNull(response);
        assertEquals("mocked-jwt-token-2026", response.getToken());
        assertEquals("admin", response.getUsername());
        assertEquals("ADMIN", response.getRol());
    }
}