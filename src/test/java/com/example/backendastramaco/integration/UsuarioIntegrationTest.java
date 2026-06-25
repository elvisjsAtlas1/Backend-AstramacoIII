package com.example.backendastramaco.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UsuarioIntegrationTest extends UsuarioBaseIntegrationTest {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;

    // Helper para obtener token de administrador antes de cada prueba
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
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.split("\"token\":\"")[1].split("\"")[0];
    }

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtenerTokenAdmin();
    }

    @Test
    @Order(1)
    @DisplayName("Debe crear un usuario nuevo exitosamente (201 Created)")
    void crear_DebeRegistrarNuevoUsuario() throws Exception {
        String body = """
            {
              "username": "nuevo.usuario",
              "password": "Password123!",
              "rol": "USER"
            }
            """;

        mockMvc.perform(post("/api/usuarios")
                        .header(AUTHORIZATION, BEARER_PREFIX + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.username").value("nuevo.usuario"))
                .andExpect(jsonPath("$.rol").value("USER"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @Order(2)
    @DisplayName("Debe listar todos los usuarios paginados (200 OK)")
    void listar_DebeRetornarUsuariosPaginados() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .header(AUTHORIZATION, BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)))
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    @Order(3)
    @DisplayName("Debe obtener un usuario por su ID (200 OK)")
    void obtenerPorId_DebeRetornarUsuario() throws Exception {
        // 1. Crear usuario
        String body = """
            {
              "username": "usuario.id",
              "password": "Password123!",
              "rol": "TRANSPORTISTA"
            }
            """;

        String responseStr = mockMvc.perform(post("/api/usuarios")
                        .header(AUTHORIZATION, BEARER_PREFIX + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String usuarioId = responseStr.split("\"id\":")[1].split(",")[0].trim();

        // 2. Obtener por ID
        mockMvc.perform(get("/api/usuarios/" + usuarioId)
                        .header(AUTHORIZATION, BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Integer.parseInt(usuarioId)))
                .andExpect(jsonPath("$.username").value("usuario.id"));
    }

    @Test
    @Order(4)
    @DisplayName("Debe obtener un usuario por su username (200 OK)")
    void obtenerPorUsername_DebeRetornarUsuario() throws Exception {
        String usernameBuscar = "admin"; // Ya existe por el DataInitializer o el BaseTest

        mockMvc.perform(get("/api/usuarios/username/" + usernameBuscar)
                        .header(AUTHORIZATION, BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(usernameBuscar));
    }

    @Test
    @Order(5)
    @DisplayName("Debe actualizar los datos de un usuario (200 OK)")
    void actualizar_DebeModificarDatosDelUsuario() throws Exception {
        // 1. Crear usuario
        String bodyCrear = """
            {
              "username": "usuario.actualizar",
              "password": "Password123!",
              "rol": "USER"
            }
            """;

        String responseStr = mockMvc.perform(post("/api/usuarios")
                        .header(AUTHORIZATION, BEARER_PREFIX + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCrear))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String usuarioId = responseStr.split("\"id\":")[1].split(",")[0].trim();

        // 2. Actualizar usuario (cambio de rol)
        String bodyActualizar = """
            {
              "username": "usuario.actualizado",
              "password": "NewPassword123!",
              "rol": "ADMIN"
            }
            """;

        mockMvc.perform(put("/api/usuarios/" + usuarioId)
                        .header(AUTHORIZATION, BEARER_PREFIX + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyActualizar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("usuario.actualizado"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    @Order(6)
    @DisplayName("Debe cambiar el estado activo/inactivo del usuario (200 OK)")
    void cambiarEstado_DebeActualizarEstado() throws Exception {
        // 1. Crear usuario
        String body = """
            {
              "username": "usuario.estado",
              "password": "Password123!",
              "rol": "USER"
            }
            """;

        String responseStr = mockMvc.perform(post("/api/usuarios")
                        .header(AUTHORIZATION, BEARER_PREFIX + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String usuarioId = responseStr.split("\"id\":")[1].split(",")[0].trim();

        // 2. Cambiar estado a false
        mockMvc.perform(patch("/api/usuarios/" + usuarioId + "/estado")
                        .param("activo", "false")
                        .header(AUTHORIZATION, BEARER_PREFIX + adminToken))
                .andExpect(status().isOk());

        // Verificar que se cambió
        mockMvc.perform(get("/api/usuarios/" + usuarioId)
                        .header(AUTHORIZATION, BEARER_PREFIX + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    @Order(7)
    @DisplayName("Debe eliminar lógicamente un usuario (204 No Content)")
    void eliminar_DebeRealizarSoftDelete() throws Exception {
        // 1. Crear usuario
        String body = """
            {
              "username": "usuario.eliminar",
              "password": "Password123!",
              "rol": "USER"
            }
            """;

        String responseStr = mockMvc.perform(post("/api/usuarios")
                        .header(AUTHORIZATION, BEARER_PREFIX + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String usuarioId = responseStr.split("\"id\":")[1].split(",")[0].trim();

        // 2. Eliminar (Soft Delete)
        mockMvc.perform(delete("/api/usuarios/" + usuarioId)
                        .header(AUTHORIZATION, BEARER_PREFIX + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(8)
    @DisplayName("Debe restaurar un usuario eliminado lógicamente (204 No Content)")
    void restaurar_DebeQuitarSoftDelete() throws Exception {
        // 1. Crear usuario
        String body = """
            {
              "username": "usuario.restaurar",
              "password": "Password123!",
              "rol": "USER"
            }
            """;

        String responseStr = mockMvc.perform(post("/api/usuarios")
                        .header(AUTHORIZATION, BEARER_PREFIX + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String usuarioId = responseStr.split("\"id\":")[1].split(",")[0].trim();

        // 2. Eliminar lógicamente
        mockMvc.perform(delete("/api/usuarios/" + usuarioId)
                        .header(AUTHORIZATION, BEARER_PREFIX + adminToken))
                .andExpect(status().isNoContent());

        // 3. Restaurar
        mockMvc.perform(patch("/api/usuarios/" + usuarioId + "/restaurar")
                        .header(AUTHORIZATION, BEARER_PREFIX + adminToken))
                .andExpect(status().isNoContent());
    }
}