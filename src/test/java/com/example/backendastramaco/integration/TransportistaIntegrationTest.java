package com.example.backendastramaco.integration;

import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.UsuarioRepository;
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
class TransportistaIntegrationTest extends TransportistaBaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransportistaRepository transportistaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private String adminToken;

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

        // Extraer token del JSON response
        return response.split("\"token\":\"")[1].split("\"")[0];
    }

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtenerTokenAdmin();
    }

    // ========== PRUEBAS DE CREAR ==========

    @Test
    @Order(1)
    @DisplayName("POST /api/transportistas - Debe crear transportista correctamente")
    void crear_DebeCrearTransportistaCorrectamente() throws Exception {
        String body = """
            {
              "nombre": "Carlos",
              "apellidos": "Mamani",
              "dni": "12345678",
              "edad": 30,
              "tipoTransporte": "CAMIONERO",
              "placa": "ABC-123",
              "vehiculoInfo": "Camion azul",
              "capacidad": 12.5,
              "estado": "ACTIVO"
            }
            """;

        mockMvc.perform(post("/api/transportistas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nombre").value("Carlos"))
                .andExpect(jsonPath("$.apellidos").value("Mamani"))
                .andExpect(jsonPath("$.dni").value("12345678"))
                .andExpect(jsonPath("$.tipoTransporte").value("CAMIONERO"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/transportistas - Debe asignar estado ACTIVO por defecto")
    void crear_DebeAsignarEstadoActivo_CuandoEstadoEsNull() throws Exception {
        String body = """
            {
              "nombre": "Luis",
              "apellidos": "Quispe",
              "dni": "87654321",
              "edad": 28,
              "tipoTransporte": "VOLQUETERO",
              "placa": "XYZ-987",
              "vehiculoInfo": "Volquete rojo",
              "capacidad": 20.0
            }
            """;

        mockMvc.perform(post("/api/transportistas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Luis"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/transportistas - Debe lanzar error cuando no hay autenticación")
    void crear_DebeLanzarError_CuandoNoHayToken() throws Exception {
        String body = """
            {
              "nombre": "Carlos",
              "apellidos": "Mamani",
              "dni": "12345678",
              "edad": 30,
              "tipoTransporte": "CAMIONERO",
              "placa": "ABC-123"
            }
            """;

        mockMvc.perform(post("/api/transportistas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ========== PRUEBAS DE LISTAR ==========

    @Test
    @Order(4)
    @DisplayName("GET /api/transportistas - Debe listar transportistas")
    void listar_DebeRetornarTransportistas() throws Exception {
        mockMvc.perform(get("/api/transportistas")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    // ========== PRUEBAS DE LISTAR POR TIPO ==========

    @Test
    @Order(5)
    @DisplayName("GET /api/transportistas/tipo/CAMIONERO - Debe retornar camioneros")
    void listarPorTipo_DebeRetornarCamioneros() throws Exception {
        mockMvc.perform(get("/api/transportistas/tipo/CAMIONERO")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/transportistas/tipo/VOLQUETERO - Debe retornar volqueteros")
    void listarPorTipo_DebeRetornarVolqueteros() throws Exception {
        mockMvc.perform(get("/api/transportistas/tipo/VOLQUETERO")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    // ========== PRUEBAS DE DOCUMENTOS ==========

    @Test
    @Order(7)
    @DisplayName("GET /api/transportistas/{id}/documentos - Debe listar documentos (puede estar vacío)")
    void documentos_DebeListarDocumentos() throws Exception {
        // Primero crear un transportista
        String crearBody = """
            {
              "nombre": "Documentos",
              "apellidos": "Test",
              "dni": "11111111",
              "edad": 25,
              "tipoTransporte": "CAMIONERO",
              "placa": "DOC-001",
              "vehiculoInfo": "Test",
              "capacidad": 10.0,
              "estado": "ACTIVO"
            }
            """;

        String response = mockMvc.perform(post("/api/transportistas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String idStr = response.split("\"id\":")[1].split(",")[0];
        Long transportistaId = Long.parseLong(idStr);

        mockMvc.perform(get("/api/transportistas/" + transportistaId + "/documentos")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    // ========== PRUEBAS DE MI PERFIL ==========

    @Test
    @Order(8)
    @DisplayName("GET /api/transportistas/me - Debe obtener perfil del transportista autenticado")
    void obtenerMiPerfil_DebeRetornarPerfil() throws Exception {
        // Primero crear un transportista
        String nombreUnico = "Perfil" + System.currentTimeMillis();
        String crearBody = String.format("""
            {
              "nombre": "%s",
              "apellidos": "Test",
              "dni": "22222222",
              "edad": 25,
              "tipoTransporte": "CAMIONERO",
              "placa": "PER-001",
              "vehiculoInfo": "Test",
              "capacidad": 10.0,
              "estado": "ACTIVO"
            }
            """, nombreUnico);

        String response = mockMvc.perform(post("/api/transportistas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Obtener el username generado
        String username = response.split("\"username\":\"")[1].split("\"")[0];

        // Login como el transportista creado
        String loginBody = String.format("""
            {
              "username": "%s",
              "password": "22222222"
            }
            """, username);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String transportistaToken = loginResponse.split("\"token\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/transportistas/me")
                        .header("Authorization", "Bearer " + transportistaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nombre").value(nombreUnico));
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/transportistas/me - Debe lanzar error cuando no autenticado")
    void obtenerMiPerfil_DebeLanzarError_CuandoNoAutenticado() throws Exception {
        mockMvc.perform(get("/api/transportistas/me"))
                .andExpect(status().isUnauthorized());
    }
}