package com.example.backendastramaco.integration;

import com.example.backendastramaco.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UsuarioIntegrationTest extends BaseIntegrationTest {

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
        String loginBody = String.format("""
            {
              "username": "%s",
              "password": "%s"
            }
            """, ADMIN_USERNAME, ADMIN_PASSWORD);

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
        String body = String.format("""
            {
              "username": "%s",
              "password": "%s",
              "rol": "%s"
            }
            """, username, password, rol);

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.rol").value(rol))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @DisplayName("Debe rechazar creación de usuario sin token de autorización")
    void crear_DebeRechazarUsuarioSinToken() throws Exception {
        String body = """
        {
          "username": "usuario.sin.token",
          "password": "123456",
          "rol": "TRANSPORTISTA"
        }
        """;

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Debe rechazar creación de usuario con token inválido")
    void crear_DebeRechazarUsuarioConTokenInvalido() throws Exception {
        String invalidToken = "token.invalido.123";
        String body = """
        {
          "username": "usuario.token.invalido",
          "password": "123456",
          "rol": "TRANSPORTISTA"
        }
        """;

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Debe rechazar creación de usuario con username duplicado")
    void crear_DebeRechazarUsuarioDuplicado() throws Exception {
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
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Debe obtener usuario por ID")
    void obtenerPorId_DebeRetornarUsuario() throws Exception {
        // Crear usuario primero
        String body = """
            {
              "username": "usuario.obtener",
              "password": "123456",
              "rol": "USER"
            }
            """;

        String response = mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = Long.parseLong(response.split("\"id\":")[1].split(",")[0]);

        mockMvc.perform(get("/api/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.username").value("usuario.obtener"));
    }

    @Test
    @DisplayName("Debe actualizar usuario")
    void actualizar_DebeActualizarUsuario() throws Exception {
        String body = """
            {
              "username": "usuario.actualizar",
              "password": "123456",
              "rol": "USER"
            }
            """;

        String response = mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = Long.parseLong(response.split("\"id\":")[1].split(",")[0]);

        String updateBody = """
            {
              "username": "usuario.actualizado",
              "password": "654321",
              "rol": "ADMIN"
            }
            """;

        mockMvc.perform(put("/api/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("usuario.actualizado"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    @DisplayName("Debe eliminar usuario (soft delete)")
    void eliminar_DebeEliminarUsuarioLogicamente() throws Exception {
        String body = """
            {
              "username": "usuario.eliminar",
              "password": "123456",
              "rol": "USER"
            }
            """;

        String response = mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = Long.parseLong(response.split("\"id\":")[1].split(",")[0]);

        mockMvc.perform(delete("/api/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
}