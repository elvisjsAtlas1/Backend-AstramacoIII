package com.example.backendastramaco.controller;

import com.example.backendastramaco.dto.UsuarioRequestDTO;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc
class UsuarioControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN") // Simula el rol requerido para superar el filtro de SecurityConfig
    @DisplayName("Debe invocar el método crear del controlador y cubrir las asignaciones en JaCoCo")
    void crear_DebePasarPorLasLineasDelControllerCompletamente() throws Exception {
        // Arrange
        UsuarioRequestDTO dto = new UsuarioRequestDTO();
        dto.setUsername("carlos.qa");
        dto.setPassword("passwordSecure123");
        dto.setRol(Rol.TRANSPORTISTA);

        Usuario usuarioEsperado = new Usuario();
        usuarioEsperado.setUsername("carlos.qa");

        when(usuarioService.crear(any(Usuario.class))).thenReturn(usuarioEsperado);

        // Act & Assert
        // Omitimos .with(csrf()) debido a que la protección CSRF está explícitamente deshabilitada en la API
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}