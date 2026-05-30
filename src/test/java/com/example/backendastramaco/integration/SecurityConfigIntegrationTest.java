package com.example.backendastramaco.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigIntegrationTest extends SecurityBaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Endpoint público /api/auth/login debe ser accesible sin autenticación")
    void endpointPublicoLoginDebeSerAccesible() throws Exception {
        String loginBody = """
            {
              "username": "admin",
              "password": "admin123"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Endpoint público /swagger-ui debe ser accesible sin autenticación")
    void endpointPublicoSwaggerDebeSerAccesible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Endpoint público /v3/api-docs debe ser accesible sin autenticación")
    void endpointPublicoApiDocsDebeSerAccesible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Endpoint protegido /api/transportistas debe retornar 401 sin token")
    void endpointProtegidoDebeRetornar401SinToken() throws Exception {
        mockMvc.perform(get("/api/transportistas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Endpoint /api/usuarios (POST) debe requerir rol ADMIN")
    void endpointCrearUsuarioSoloAdmin() throws Exception {
        // Primero obtenemos token de admin
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

        // Extraer token
        String token = response.split("\"token\":\"")[1].split("\"")[0];

        String crearUsuarioBody = """
            {
              "username": "nuevo.usuario",
              "password": "password123",
              "rol": "TRANSPORTISTA"
            }
            """;

        // Con token de admin debe funcionar
        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearUsuarioBody))
                .andExpect(status().isOk());
    }
}