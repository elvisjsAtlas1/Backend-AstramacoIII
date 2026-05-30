package com.example.backendastramaco.integration;

import com.example.backendastramaco.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UsuarioIntegrationTest extends UsuarioBaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtenerTokenAdmin();
    }

    private String obtenerTokenAdmin() throws Exception {
        String loginBody = """
        {
          "username": "admin",
          "password": "admin123"
        }
        """;

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.split("\"token\":\"")[1].split("\"")[0];
    }

    @ParameterizedTest
    @CsvSource({
            "usuario.integration, 123456, USER",
            "usuario.password, miPassword123, USER",
            "transportista.integration, 123456, TRANSPORTISTA"
    })
    @DisplayName("Debe crear usuario con diferentes roles y credenciales")
    void crear_DebeCrearUsuarioCorrectamente(String username, String password, String rol) throws Exception {
        // Arrange
        String body = String.format("""
            {
              "username": "%s",
              "password": "%s",
              "rol": "%s"
            }
            """, username, password, rol);

        // Act & Assert
        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Debe rechazar creación de usuario sin token de autorización")
    void crear_DebeRechazarUsuarioSinToken() throws Exception {
        // Arrange
        String body = """
        {
          "username": "usuario.sin.token",
          "password": "123456",
          "rol": "USER"
        }
        """;

        // Act & Assert
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized()); // 🔥 CORRECCIÓN: Cambiado isForbidden() por isUnauthorized()
    }

    @Test
    @DisplayName("Debe rechazar creación de usuario con token inválido")
    void crear_DebeRechazarUsuarioConTokenInvalido() throws Exception {
        // Arrange
        String invalidToken = "token.invalido.123";
        String body = """
        {
          "username": "usuario.token.invalido",
          "password": "123456",
          "rol": "USER"
        }
        """;

        // Act & Assert
        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized()); // 🔥 CORRECCIÓN: Cambiado isForbidden() por isUnauthorized()
    }

    @Test
    @DisplayName("Debe rechazar creación de usuario con username duplicado")
    void crear_DebeRechazarUsuarioDuplicado() throws Exception {
        // Arrange - Crear primer usuario
        String body = """
            {
              "username": "usuario.duplicado",
              "password": "123456",
              "rol": "USER"
            }
            """;

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Act & Assert - Intentar crear el mismo usuario nuevamente
        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}