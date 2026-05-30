package com.example.backendastramaco.controller;


import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.security.jwt.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
class AuthControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authManager;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Debe ejecutar el cuerpo interno de login para cobertura de JaCoCo")
    void login_DebeEjecutarMetodoInternoCompletamente() throws Exception {
        // Arrange
        String username = "admin";
        String password = "admin123";
        String requestBody = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);

        Usuario usuarioSimulado = new Usuario();
        usuarioSimulado.setUsername(username);
        usuarioSimulado.setRol(Rol.ADMIN);

        // Forzamos el comportamiento de los componentes mockeados
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(jwtUtil.generateToken(username)).thenReturn("mocked-jwt-token-2026");
        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(usuarioSimulado));

        // Act & Assert
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)) // 🔥 Quitamos el .with(csrf()) que daba error en rojo
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token-2026"))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }
}