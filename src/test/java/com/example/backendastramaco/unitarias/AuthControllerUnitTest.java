package com.example.backendastramaco.unitarias;

import com.example.backendastramaco.controller.AuthController;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuthController authController;

    private final String loginPayload = """
            {
              "username": "admin",
              "password": "password123"
            }
            """;

    @BeforeEach
    void setUp() {
        // Configuramos MockMvc en modo standalone (solo para este controlador)
        // Esto es muchísimo más rápido que levantar todo el contexto de Spring
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("Debe retornar 200 OK y el Token cuando el login es exitoso")
    void login_CaminoFeliz_DebeRetornarToken() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        when(jwtUtil.generateToken("admin")).thenReturn("fake-jwt-token-12345");

        Usuario usuarioMock = new Usuario();
        usuarioMock.setUsername("admin");
        usuarioMock.setRol(Rol.ADMIN);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioMock));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token-12345"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    @DisplayName("Debe retornar 401 si authenticate pasa pero isAuthenticated() es false")
    void login_AutenticacionFalla_DebeRetornar401() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false); // Forzamos el bloque 'else'
        when(authManager.authenticate(any())).thenReturn(auth);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("Debe retornar 401 y capturar BadCredentialsException")
    void login_BadCredentials_DebeRetornar401() throws Exception {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("Debe retornar 401 y capturar DisabledException")
    void login_DisabledException_DebeRetornar401() throws Exception {
        when(authManager.authenticate(any())).thenThrow(new DisabledException("User disabled"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Usuario deshabilitado"));
    }

    @Test
    @DisplayName("Debe retornar 401 y capturar LockedException")
    void login_LockedException_DebeRetornar401() throws Exception {
        when(authManager.authenticate(any())).thenThrow(new LockedException("User locked"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Usuario bloqueado"));
    }

    @Test
    @DisplayName("Debe retornar 401 y capturar Exception genérica de Autenticación")
    void login_AuthenticationException_DebeRetornar401() throws Exception {
        // Creamos una subclase anónima de AuthenticationException para probar el catch
        when(authManager.authenticate(any())).thenThrow(new AuthenticationException("Error genérico de auth") {});

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("Debe retornar 500 y capturar Exception inesperada")
    void login_ExceptionInesperada_DebeRetornar500() throws Exception {
        when(authManager.authenticate(any())).thenThrow(new RuntimeException("Falla catastrófica de base de datos"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error interno del servidor"));
    }

    @Test
    @DisplayName("Debe retornar 500 si el usuario no se encuentra en la BD después del login")
    void login_UsuarioNoEncontradoEnBd_DebeRetornar500() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(authManager.authenticate(any())).thenReturn(auth);

        when(jwtUtil.generateToken("admin")).thenReturn("token");
        // Forzamos que retorne vacío para que salte el RuntimeException
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error interno del servidor"));
    }
}