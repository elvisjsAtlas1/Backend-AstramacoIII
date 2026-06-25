package com.example.backendastramaco.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuditoriaPedidoIntegrationTest extends AuditoriaPedidoBaseIntegrationTest {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    // Helper para obtener token de administrador
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

    @Test
    @DisplayName("Debe listar todas las auditorías de pedidos (200 OK)")
    void listar_DebeRetornarPaginaDeAuditorias() throws Exception {
        String token = obtenerTokenAdmin();

        mockMvc.perform(get("/api/auditoria/pedidos")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)))
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    @DisplayName("Debe listar auditorías por ID de pedido (200 OK)")
    void listarPorPedido_DebeRetornarPagina() throws Exception {
        String token = obtenerTokenAdmin();

        mockMvc.perform(get("/api/auditoria/pedidos/pedido/1")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)));
    }

    @Test
    @DisplayName("Debe listar auditorías filtradas por acción (200 OK)")
    void listarPorAccion_DebeRetornarPagina() throws Exception {
        String token = obtenerTokenAdmin();

        mockMvc.perform(get("/api/auditoria/pedidos/accion/CREATE")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)));
    }

    @Test
    @DisplayName("Debe listar auditorías por rango de fechas (200 OK)")
    void listarPorRangoFechas_DebeRetornarPagina() throws Exception {
        String token = obtenerTokenAdmin();

        // Formatear fechas en formato ISO para la petición
        String inicio = LocalDateTime.now().minusDays(7).format(DateTimeFormatter.ISO_DATE_TIME);
        String fin = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_DATE_TIME);

        mockMvc.perform(get("/api/auditoria/pedidos/fechas")
                        .param("inicio", inicio)
                        .param("fin", fin)
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)));
    }

    @Test
    @DisplayName("Debe obtener el resumen de acciones de auditoría (200 OK)")
    void getResumenAuditorias_DebeRetornarMapaConConteos() throws Exception {
        String token = obtenerTokenAdmin();

        mockMvc.perform(get("/api/auditoria/pedidos/resumen")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.CREATE").exists())
                .andExpect(jsonPath("$.UPDATE").exists())
                .andExpect(jsonPath("$.DELETE").exists())
                .andExpect(jsonPath("$.RESTORE").exists())
                .andExpect(jsonPath("$.DELETE_PERMANENT").exists())
                .andExpect(jsonPath("$.UPDATE_ESTADO").exists());
    }
}