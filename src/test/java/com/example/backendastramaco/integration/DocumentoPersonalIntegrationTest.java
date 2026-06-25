package com.example.backendastramaco.integration;

import com.example.backendastramaco.dto.TransportistaRequestDTO;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.service.TransportistaService;
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
class DocumentoPersonalIntegrationTest extends DocumentoPersonalBaseIntegrationTest {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CONTENT_TYPE_JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransportistaService transportistaService;

    // Helper para obtener token admin
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

    // Helper para crear un transportista padre
    private Long crearTransportista(String dni) {
        TransportistaRequestDTO dto = new TransportistaRequestDTO();
        dto.setNombre("Test");
        dto.setApellidos("Documento");
        dto.setDni(dni);
        dto.setEdad(35);
        dto.setTipoTransporte(TipoTransporte.CAMIONERO);
        dto.setPlaca("DOC-" + dni.substring(0, 3));
        dto.setVehiculoInfo("Vehiculo test doc");
        dto.setCapacidad(15.0);
        dto.setEstado("ACTIVO");

        return transportistaService.crear(dto).getId();
    }

    // ========== PRUEBAS DE CREACIÓN ==========

    @Test
    @Order(1)
    @DisplayName("POST /api/documentos/{transportistaId} - Debe registrar un nuevo documento exitosamente (201 Created)")
    void guardar_DebeRegistrarNuevoDocumento() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("80808080");

        String body = """
            {
              "tipoDocumento": "LICENCIA",
              "valor": "SI"
            }
            """;

        mockMvc.perform(post("/api/documentos/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.tipoDocumento").value("LICENCIA"))
                .andExpect(jsonPath("$.valor").value("SI"));
    }

    // ========== PRUEBAS DE LECTURA ==========

    @Test
    @Order(2)
    @DisplayName("GET /api/documentos/transportista/{id} - Debe listar documentos de un transportista (200 OK)")
    void listar_DebeRetornarListaDeDocumentos() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("81818181");

        String body = """
            {
              "tipoDocumento": "DNI",
              "valor": "81818181",
              "fechaEmision": "2020-05-10",
              "fechaVencimiento": "2030-05-10"
            }
            """;

        mockMvc.perform(post("/api/documentos/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/documentos/transportista/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].tipoDocumento").value("DNI"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/documentos/transportista/{id}/paginado - Debe listar documentos paginados (200 OK)")
    void listarPaginado_DebeRetornarPaginaDeDocumentos() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("85858585");

        mockMvc.perform(get("/api/documentos/transportista/" + transportistaId + "/paginado")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)))
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/documentos/{id} - Debe obtener documento por ID")
    void obtenerPorId_DebeRetornarDocumento() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("86868686");

        String body = """
            {
              "tipoDocumento": "SOAT",
              "valor": "SOAT-123",
              "fechaEmision": "2023-01-01",
              "fechaVencimiento": "2024-01-01"
            }
            """;

        String responseStr = mockMvc.perform(post("/api/documentos/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String docId = responseStr.split("\"id\":")[1].split(",")[0].trim();

        mockMvc.perform(get("/api/documentos/" + docId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Integer.parseInt(docId)));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/documentos/transportista/{id}/activos - Debe listar solo documentos activos")
    void listarActivos_DebeRetornarLista() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("87878787");

        mockMvc.perform(get("/api/documentos/transportista/" + transportistaId + "/activos")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/documentos/me - Debe listar mis documentos")
    void misDocumentos_DebeRetornarLista() throws Exception {
        String tokenAdmin = obtenerTokenAdmin();

        // Crear transportista para obtener su usuario
        String bodyTransportista = """
            {
              "nombre": "DocMe",
              "apellidos": "Test",
              "dni": "88888888",
              "edad": 35,
              "tipoTransporte": "CAMIONERO",
              "placa": "DOC-888",
              "vehiculoInfo": "Vehiculo test doc",
              "capacidad": 15.0,
              "estado": "ACTIVO"
            }
            """;

        String responseTrans = mockMvc.perform(post("/api/transportistas")
                        .header(AUTHORIZATION, BEARER_PREFIX + tokenAdmin)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(bodyTransportista))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String username = responseTrans.split("\"username\":\"")[1].split("\"")[0];

        // Login con el transportista creado
        String loginBody = String.format("""
            {
              "username": "%s",
              "password": "88888888"
            }
            """, username);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String tokenTrans = loginResponse.split("\"token\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/documentos/me")
                        .header(AUTHORIZATION, BEARER_PREFIX + tokenTrans))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    // ========== PRUEBAS DE ACTUALIZACIÓN ==========

    @Test
    @Order(7)
    @DisplayName("PUT /api/documentos/{id} - Debe actualizar un documento existente (200 OK)")
    void actualizar_DebeModificarDatosDelDocumento() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("82828282");

        String bodyCrear = """
            {
              "tipoDocumento": "SOAT",
              "valor": "SOAT-001",
              "fechaEmision": "2023-01-01",
              "fechaVencimiento": "2024-01-01"
            }
            """;

        String responseStr = mockMvc.perform(post("/api/documentos/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(bodyCrear))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String docId = responseStr.split("\"id\":")[1].split(",")[0].trim();

        String bodyActualizar = """
            {
              "tipoDocumento": "SOAT",
              "valor": "SOAT-002-ACTUALIZADO",
              "fechaEmision": "2024-01-01",
              "fechaVencimiento": "2025-01-01"
            }
            """;

        mockMvc.perform(put("/api/documentos/" + docId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(bodyActualizar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value("SOAT-002-ACTUALIZADO"));
    }

    @Test
    @Order(8)
    @DisplayName("PATCH /api/documentos/{id}/estado - Debe cambiar el estado de un documento (200 OK)")
    void cambiarEstado_DebeActivarODesactivarDocumento() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("83838383");

        String body = """
            {
              "tipoDocumento": "REVISION_TECNICA",
              "valor": "REV-999",
              "fechaEmision": "2023-01-01",
              "fechaVencimiento": "2024-01-01"
            }
            """;

        String responseStr = mockMvc.perform(post("/api/documentos/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String docId = responseStr.split("\"id\":")[1].split(",")[0].trim();

        mockMvc.perform(patch("/api/documentos/" + docId + "/estado")
                        .param("activo", "false")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk());
    }

    // ========== PRUEBAS DE ELIMINACIÓN Y RESTAURACIÓN ==========

    @Test
    @Order(9)
    @DisplayName("DELETE /api/documentos/{id} - Debe eliminar un documento lógicamente (204 No Content)")
    void eliminar_DebeRealizarSoftDelete() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("84848484");

        String body = """
            {
              "tipoDocumento": "TARJETA_CIRCULACION",
              "valor": "SI"
            }
            """;

        String responseStr = mockMvc.perform(post("/api/documentos/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String docId = responseStr.split("\"id\":")[1].split(",")[0].trim();

        mockMvc.perform(delete("/api/documentos/" + docId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(10)
    @DisplayName("PATCH /api/documentos/{id}/restaurar - Debe restaurar un documento lógicamente (204 No Content)")
    void restaurar_DebeQuitarSoftDelete() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("89898989");

        String body = """
            {
              "tipoDocumento": "LICENCIA",
              "valor": "SI"
            }
            """;

        String responseStr = mockMvc.perform(post("/api/documentos/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String docId = responseStr.split("\"id\":")[1].split(",")[0].trim();

        // Eliminar
        mockMvc.perform(delete("/api/documentos/" + docId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isNoContent());

        // Restaurar
        mockMvc.perform(patch("/api/documentos/" + docId + "/restaurar")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(11)
    @DisplayName("DELETE /api/documentos/{id}/permanente - Debe eliminar un documento físicamente (204 No Content)")
    void eliminarPermanente_DebeBorrarFisicamente() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("91919192");

        String body = """
            {
              "tipoDocumento": "LICENCIA",
              "valor": "SI"
            }
            """;

        String responseStr = mockMvc.perform(post("/api/documentos/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String docId = responseStr.split("\"id\":")[1].split(",")[0].trim();

        mockMvc.perform(delete("/api/documentos/" + docId + "/permanente")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isNoContent());
    }
}