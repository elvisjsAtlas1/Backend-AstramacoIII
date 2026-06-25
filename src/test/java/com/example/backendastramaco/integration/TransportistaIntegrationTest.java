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

        return response.split("\"token\":\"")[1].split("\"")[0];
    }

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtenerTokenAdmin();
    }

    // ========== PRUEBAS DE CREAR ==========

    @Test
    @Order(1)
    @DisplayName("POST /api/transportistas - Debe crear transportista correctamente (201 Created)")
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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nombre").value("Carlos"));
    }

    // ========== PRUEBAS DE LISTAR Y OBTENER ==========

    @Test
    @Order(2)
    @DisplayName("GET /api/transportistas - Debe listar transportistas con paginación")
    void listar_DebeRetornarTransportistas() throws Exception {
        mockMvc.perform(get("/api/transportistas")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)))
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/transportistas/todos - Debe listar todos (activos y eliminados)")
    void listarTodos_DebeRetornarPagina() throws Exception {
        mockMvc.perform(get("/api/transportistas/todos")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/transportistas/eliminados - Debe listar eliminados")
    void listarEliminados_DebeRetornarPagina() throws Exception {
        mockMvc.perform(get("/api/transportistas/eliminados")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/transportistas/{id} - Debe obtener por ID")
    void obtenerPorId_DebeRetornarTransportista() throws Exception {
        // Obtenemos el transportista creado en el Test 1 (ID = 1 normalmente)
        mockMvc.perform(get("/api/transportistas/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/transportistas/dni/{dni} - Debe obtener por DNI")
    void obtenerPorDni_DebeRetornarTransportista() throws Exception {
        mockMvc.perform(get("/api/transportistas/dni/12345678")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("12345678"));
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/transportistas/usuario/{usuarioId} - Debe obtener por ID de usuario")
    void obtenerPorUsuario_DebeRetornarTransportista() throws Exception {
        // En tu lógica, el ID del usuario suele ser 2 para el primer transportista creado (el admin es 1)
        mockMvc.perform(get("/api/transportistas/usuario/2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/transportistas/tipo/{tipo} - Debe retornar por tipo")
    void listarPorTipo_DebeRetornarCamioneros() throws Exception {
        mockMvc.perform(get("/api/transportistas/tipo/CAMIONERO")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    // ========== PRUEBAS DE ACTUALIZACIÓN Y ESTADOS ==========

    @Test
    @Order(9)
    @DisplayName("PUT /api/transportistas/{id} - Debe actualizar datos")
    void actualizar_DebeModificarDatos() throws Exception {
        String bodyActualizado = """
            {
              "nombre": "Carlos Modificado",
              "apellidos": "Mamani",
              "dni": "12345678",
              "edad": 35,
              "tipoTransporte": "CAMIONERO",
              "placa": "ABC-123",
              "vehiculoInfo": "Camion azul modificado",
              "capacidad": 15.0,
              "estado": "ACTIVO"
            }
            """;

        mockMvc.perform(put("/api/transportistas/1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyActualizado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Carlos Modificado"))
                .andExpect(jsonPath("$.edad").value(35));
    }

    @Test
    @Order(10)
    @DisplayName("PATCH /api/transportistas/{id}/estado - Debe cambiar estado")
    void cambiarEstado_DebeActualizarEstado() throws Exception {
        mockMvc.perform(patch("/api/transportistas/1/estado")
                        .param("estado", "INACTIVO")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ========== PRUEBAS DE DOCUMENTOS Y PERFIL ==========

    @Test
    @Order(11)
    @DisplayName("GET /api/transportistas/{id}/documentos - Debe listar documentos")
    void documentos_DebeListarDocumentos() throws Exception {
        mockMvc.perform(get("/api/transportistas/1/documentos")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/transportistas/me - Debe obtener perfil del autenticado")
    void obtenerMiPerfil_DebeRetornarPerfil() throws Exception {
        // Crear un usuario específico para esto
        String body = """
            {
              "nombre": "Perfil",
              "apellidos": "Test",
              "dni": "99999999",
              "edad": 25,
              "tipoTransporte": "CAMIONERO",
              "placa": "PER-001",
              "vehiculoInfo": "Test",
              "capacidad": 10.0,
              "estado": "ACTIVO"
            }
            """;

        String response = mockMvc.perform(post("/api/transportistas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String username = response.split("\"username\":\"")[1].split("\"")[0];

        // Login con ese usuario
        String loginBody = String.format("""
            {
              "username": "%s",
              "password": "99999999"
            }
            """, username);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String transportistaToken = loginResponse.split("\"token\":\"")[1].split("\"")[0];

        // Petición /me
        mockMvc.perform(get("/api/transportistas/me")
                        .header("Authorization", "Bearer " + transportistaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Perfil"));
    }

    // ========== PRUEBAS DE ELIMINACIÓN ==========

    @Test
    @Order(13)
    @DisplayName("DELETE /api/transportistas/{id} - Debe eliminar lógicamente (204 No Content)")
    void eliminar_DebeRealizarSoftDelete() throws Exception {
        mockMvc.perform(delete("/api/transportistas/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(14)
    @DisplayName("PATCH /api/transportistas/{id}/restaurar - Debe restaurar lógicamente (204 No Content)")
    void restaurar_DebeQuitarSoftDelete() throws Exception {
        mockMvc.perform(patch("/api/transportistas/1/restaurar")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(15)
    @DisplayName("DELETE /api/transportistas/{id}/permanente - Debe borrar físicamente (204 No Content)")
    void eliminarPermanente_DebeBorrarDeBD() throws Exception {
        mockMvc.perform(delete("/api/transportistas/1/permanente")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
}