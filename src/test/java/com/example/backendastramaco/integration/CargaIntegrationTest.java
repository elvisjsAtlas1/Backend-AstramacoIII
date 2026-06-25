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
class CargaIntegrationTest extends CargaBaseIntegrationTest {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CONTENT_TYPE_JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransportistaService transportistaService;

    // Helper para obtener token
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

    // Helper para crear transportista
    private Long crearTransportista(String dni, TipoTransporte tipo) {
        TransportistaRequestDTO dto = new TransportistaRequestDTO();
        dto.setNombre("Test Carga");
        dto.setApellidos("Camionero");
        dto.setDni(dni);
        dto.setEdad(30);
        dto.setTipoTransporte(tipo);
        dto.setPlaca("CAR-" + dni.substring(0, 3));
        dto.setVehiculoInfo("Vehiculo carga test");
        dto.setCapacidad(20.0);
        dto.setEstado("ACTIVO");

        return transportistaService.crear(dto).getId();
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/cargas/{transportistaId} - Debe crear carga (201 Created)")
    void crearCarga_DebeRegistrarCarga() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("10101010", TipoTransporte.CAMIONERO);

        String body = """
            {
              "tipoMaterial": "PANDERETA",
              "cantidadDisponible": 100.0
            }
            """;

        mockMvc.perform(post("/api/cargas/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.tipoMaterial").value("PANDERETA"))
                .andExpect(jsonPath("$.cantidadDisponible").value(100.0));
    }

    @Test
    @Order(2)
    @DisplayName("PUT /api/cargas/{transportistaId} - Debe actualizar carga (200 OK)")
    void actualizarCarga_DebeModificarDatos() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("20202020", TipoTransporte.CAMIONERO);

        // 1. Crear
        String bodyCrear = """
            {
              "tipoMaterial": "TECHO",
              "cantidadDisponible": 50.0
            }
            """;
        mockMvc.perform(post("/api/cargas/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(bodyCrear))
                .andExpect(status().isCreated());

        // 2. Actualizar
        String bodyActualizar = """
            {
              "tipoMaterial": "TECHO",
              "cantidadDisponible": 150.0
            }
            """;
        mockMvc.perform(put("/api/cargas/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(bodyActualizar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadDisponible").value(150.0));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/cargas/{transportistaId}/aumentar - Debe sumar cantidad (200 OK)")
    void aumentarCarga_DebeSumarCantidad() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("30303030", TipoTransporte.CAMIONERO);

        String cargaInicial = """
            {
              "tipoMaterial": "PANDERETA",
              "cantidadDisponible": 50.0
            }
            """;
        mockMvc.perform(post("/api/cargas/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(cargaInicial))
                .andExpect(status().isCreated());

        String aumentar = """
            {
              "tipoMaterial": "PANDERETA",
              "cantidadAgregar": 25.0
            }
            """;
        mockMvc.perform(post("/api/cargas/" + transportistaId + "/aumentar")
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(aumentar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadDisponible").value(75.0));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/cargas/{transportistaId} - Debe obtener carga actual")
    void obtenerCarga_DebeRetornarCargaDelTransportista() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("40404040", TipoTransporte.CAMIONERO);

        String body = """
            {
              "tipoMaterial": "TECHO",
              "cantidadDisponible": 80.0
            }
            """;
        mockMvc.perform(post("/api/cargas/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cargas/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoMaterial").value("TECHO"))
                .andExpect(jsonPath("$.cantidadDisponible").value(80.0));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/cargas/{id}/detalle - Debe obtener carga por su ID")
    void obtenerPorId_DebeRetornarDetalle() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("50505050", TipoTransporte.CAMIONERO);

        String body = """
            {
              "tipoMaterial": "PANDERETA",
              "cantidadDisponible": 30.0
            }
            """;
        String responseStr = mockMvc.perform(post("/api/cargas/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String cargaId = responseStr.split("\"id\":")[1].split(",")[0].trim();

        mockMvc.perform(get("/api/cargas/" + cargaId + "/detalle")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Integer.parseInt(cargaId)));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/cargas - Debe listar todas las cargas (paginadas)")
    void listarTodas_DebeRetornarPagina() throws Exception {
        String token = obtenerTokenAdmin();

        mockMvc.perform(get("/api/cargas")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)))
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/cargas/transportista/{id}/historial - Debe listar historial")
    void listarHistorial_DebeRetornarPagina() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("60606060", TipoTransporte.CAMIONERO);

        mockMvc.perform(get("/api/cargas/transportista/" + transportistaId + "/historial")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)));
    }

    @Test
    @Order(8)
    @DisplayName("DELETE /api/cargas/{id} - Debe eliminar lógicamente (204 No Content)")
    void eliminar_DebeRealizarSoftDelete() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("70707070", TipoTransporte.CAMIONERO);

        String body = """
            {
              "tipoMaterial": "TECHO",
              "cantidadDisponible": 10.0
            }
            """;
        String responseStr = mockMvc.perform(post("/api/cargas/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String cargaId = responseStr.split("\"id\":")[1].split(",")[0].trim();

        mockMvc.perform(delete("/api/cargas/" + cargaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(9)
    @DisplayName("PATCH /api/cargas/{id}/restaurar - Debe restaurar carga")
    void restaurar_DebeQuitarSoftDelete() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("80808080", TipoTransporte.CAMIONERO);

        String body = """
            {
              "tipoMaterial": "PANDERETA",
              "cantidadDisponible": 15.0
            }
            """;
        String responseStr = mockMvc.perform(post("/api/cargas/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String cargaId = responseStr.split("\"id\":")[1].split(",")[0].trim();

        // Eliminar
        mockMvc.perform(delete("/api/cargas/" + cargaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isNoContent());

        // Restaurar
        mockMvc.perform(patch("/api/cargas/" + cargaId + "/restaurar")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(10)
    @DisplayName("DELETE /api/cargas/{id}/permanente - Debe borrar físicamente")
    void eliminarPermanente_DebeBorrarDeBD() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("90909090", TipoTransporte.CAMIONERO);

        String body = """
            {
              "tipoMaterial": "TECHO",
              "cantidadDisponible": 99.0
            }
            """;
        String responseStr = mockMvc.perform(post("/api/cargas/" + transportistaId)
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String cargaId = responseStr.split("\"id\":")[1].split(",")[0].trim();

        mockMvc.perform(delete("/api/cargas/" + cargaId + "/permanente")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isNoContent());
    }
}