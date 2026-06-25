package com.example.backendastramaco.integration;

import com.example.backendastramaco.dto.TransportistaRequestDTO;
import com.example.backendastramaco.model.Carga;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.enums.TipoMaterial;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.CargaRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
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
class PedidoIntegrationTest extends PedidoBaseIntegrationTest {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CONTENT_TYPE_JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String TOKEN_SPLIT_REGEX = "\"token\":\"";
    private static final String TOKEN_SPLIT_END = "\"";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransportistaService transportistaService;

    @Autowired
    private TransportistaRepository transportistaRepository;

    @Autowired
    private CargaRepository cargaRepository;

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

        return response.split(TOKEN_SPLIT_REGEX)[1].split(TOKEN_SPLIT_END)[0];
    }

    private Long crearTransportista(String dni, TipoTransporte tipoTransporte) {
        TransportistaRequestDTO dto = new TransportistaRequestDTO();
        dto.setNombre("Test");
        dto.setApellidos("Pedido");
        dto.setDni(dni);
        dto.setEdad(40);
        dto.setTipoTransporte(tipoTransporte);
        dto.setPlaca("PED-" + dni.substring(0, 3));
        dto.setVehiculoInfo("Vehiculo test pedido");
        dto.setCapacidad(1000.0);
        dto.setEstado("ACTIVO");

        return transportistaService.crear(dto).getId();
    }

    private void asignarCargaTransportista(Long transportistaId, TipoMaterial material, Double cantidad) {
        Transportista transportista = transportistaRepository.findById(transportistaId).orElseThrow();
        Carga carga = Carga.builder()
                .transportista(transportista)
                .tipoMaterial(material)
                .cantidadDisponible(cantidad)
                .build();
        cargaRepository.save(carga);
    }

    // ========== PRUEBAS DE CREAR Y OBTENER ==========

    @Test
    @Order(1)
    @DisplayName("POST /api/pedidos - Debe crear un nuevo pedido exitosamente (201 Created)")
    void crear_DebeRegistrarNuevoPedido() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("90909090", TipoTransporte.CAMIONERO);
        asignarCargaTransportista(transportistaId, TipoMaterial.PANDERETA, 1000.0);

        String body = String.format("""
            {
              "transportistaId": %d,
              "clienteNombre": "Juan Perez",
              "clienteTelefono": "987654321",
              "direccionEnvio": "Av. Las Americas 123",
              "tipoTransporte": "CAMIONERO",
              "material": "PANDERETA",
              "cantidad": 500.0,
              "montoTotal": 250.0,
              "adelanto": 50.0,
              "piso": 1
            }
            """, transportistaId);

        mockMvc.perform(post("/api/pedidos")
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.clienteNombre").value("Juan Perez"));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/pedidos - Debe listar todos los pedidos con paginación")
    void listar_DebeRetornarPaginaDePedidos() throws Exception {
        String token = obtenerTokenAdmin();
        mockMvc.perform(get("/api/pedidos")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)))
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/pedidos/todos - Debe listar todos incluyendo eliminados")
    void listarTodos_DebeRetornarPagina() throws Exception {
        String token = obtenerTokenAdmin();
        mockMvc.perform(get("/api/pedidos/todos")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/pedidos/eliminados - Debe listar pedidos eliminados")
    void listarEliminados_DebeRetornarPagina() throws Exception {
        String token = obtenerTokenAdmin();
        mockMvc.perform(get("/api/pedidos/eliminados")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/pedidos/{id} - Debe obtener pedido por ID")
    void obtenerPorId_DebeRetornarPedido() throws Exception {
        String token = obtenerTokenAdmin();
        // Asumiendo que el pedido creado en el test 1 tiene ID 1
        mockMvc.perform(get("/api/pedidos/1")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ========== PRUEBAS DE PERFIL Y ACTUALIZACIÓN ==========

    @Test
    @Order(6)
    @DisplayName("GET /api/pedidos/me - Debe listar mis pedidos")
    void listarMisPedidos_DebeRetornarPagina() throws Exception {
        String token = obtenerTokenAdmin();

        // Creamos un transportista y usuario específico para asegurar que tenga pedidos
        Long transportistaId = crearTransportista("91919191", TipoTransporte.VOLQUETERO);
        asignarCargaTransportista(transportistaId, TipoMaterial.DESMONTE, 1000.0);

        String bodyPedido = String.format("""
            {
              "transportistaId": %d,
              "clienteNombre": "Maria Garcia",
              "clienteTelefono": "999888777",
              "direccionEnvio": "Calle Luna 45",
              "tipoTransporte": "VOLQUETERO",
              "material": "DESMONTE",
              "cantidad": 100.0,
              "montoTotal": 500.0,
              "adelanto": 0.0,
              "piso": 2
            }
            """, transportistaId);

        mockMvc.perform(post("/api/pedidos")
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(bodyPedido))
                .andExpect(status().isCreated());

        // El transportista creado genera un usuario "test.pedido"
        String loginBody = """
            {
              "username": "test.pedido1",
              "password": "91919191"
            }
            """;

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String transportistaToken = loginResponse.split("\"token\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/pedidos/me")
                        .header(AUTHORIZATION, BEARER_PREFIX + transportistaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)));
    }

    @Test
    @Order(7)
    @DisplayName("PUT /api/pedidos/{id} - Debe actualizar datos del pedido")
    void actualizar_DebeModificarDatos() throws Exception {
        String token = obtenerTokenAdmin();
        Long transportistaId = crearTransportista("92929292", TipoTransporte.CAMIONERO);
        asignarCargaTransportista(transportistaId, TipoMaterial.TECHO, 1000.0);

        String body = String.format("""
            {
              "transportistaId": %d,
              "clienteNombre": "Carlos Actualizado",
              "clienteTelefono": "911222333",
              "direccionEnvio": "Jr. Sol 88",
              "tipoTransporte": "CAMIONERO",
              "material": "TECHO",
              "cantidad": 15.0,
              "montoTotal": 300.0,
              "adelanto": 100.0,
              "piso": 1
            }
            """, transportistaId);

        mockMvc.perform(put("/api/pedidos/1")
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteNombre").value("Carlos Actualizado"));
    }

    @Test
    @Order(8)
    @DisplayName("PATCH /api/pedidos/{id}/estado - Debe cambiar estado")
    void cambiarEstado_DebeActualizarEstado() throws Exception {
        String token = obtenerTokenAdmin();
        mockMvc.perform(patch("/api/pedidos/1/estado")
                        .param("estado", "ENTREGADO")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));
    }

    // ========== PRUEBAS DE ELIMINACIÓN Y RESTAURACIÓN ==========

    @Test
    @Order(9)
    @DisplayName("DELETE /api/pedidos/{id} - Debe eliminar lógicamente (204 No Content)")
    void eliminar_DebeRealizarSoftDelete() throws Exception {
        String token = obtenerTokenAdmin();
        mockMvc.perform(delete("/api/pedidos/1")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(10)
    @DisplayName("PATCH /api/pedidos/{id}/restaurar - Debe restaurar lógicamente")
    void restaurar_DebeQuitarSoftDelete() throws Exception {
        String token = obtenerTokenAdmin();
        mockMvc.perform(patch("/api/pedidos/1/restaurar")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(11)
    @DisplayName("DELETE /api/pedidos/{id}/permanente - Debe borrar físicamente")
    void eliminarPermanente_DebeBorrarDeBD() throws Exception {
        String token = obtenerTokenAdmin();
        mockMvc.perform(delete("/api/pedidos/1/permanente")
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isNoContent());
    }
}