package com.example.backendastramaco.integration;

import com.example.backendastramaco.dto.CargaRequestDTO;
import com.example.backendastramaco.dto.TransportistaRequestDTO;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.enums.TipoMaterial;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.CargaRepository;
import com.example.backendastramaco.repository.PedidoRepository;
import com.example.backendastramaco.service.CargaService;
import com.example.backendastramaco.service.TransportistaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PedidoIntegrationTest extends PedidoBaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransportistaService transportistaService;

    @Autowired
    private CargaService cargaService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private CargaRepository cargaRepository;

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

    private Transportista crearTransportista(String nombre, String apellidos, String dni, TipoTransporte tipo) {
        TransportistaRequestDTO dto = new TransportistaRequestDTO();
        dto.setNombre(nombre);
        dto.setApellidos(apellidos);
        dto.setDni(dni);
        dto.setEdad(30);
        dto.setTipoTransporte(tipo);
        dto.setPlaca("PED-" + dni.substring(0, 3));
        dto.setVehiculoInfo("Vehiculo pedido test");
        dto.setCapacidad(25.0);
        dto.setEstado("ACTIVO");
        dto.setUsuarioId(1L);

        return transportistaService.crear(dto);
    }

    private void registrarCargaCamionero(Long transportistaId, TipoMaterial material, Double cantidad) {
        // Solo camioneros pueden tener carga
        CargaRequestDTO dto = new CargaRequestDTO();
        dto.setTipoMaterial(material);
        dto.setCantidadDisponible(cantidad);
        cargaService.subirCargaActual(transportistaId, dto);
    }

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtenerTokenAdmin();
        pedidoRepository.deleteAll();
        cargaRepository.deleteAll();
    }

    // ========== PRUEBA 1 - CREAR PEDIDO CAMIONERO ==========

    @Test
    @Order(1)
    @DisplayName("POST /api/pedidos - Debe crear pedido camionero y descontar carga")
    void crearPedido_DebeCrearPedidoCamioneroYDescontarCarga() throws Exception {
        Transportista transportista = crearTransportista("Pedro", "Pedido", "12121212", TipoTransporte.CAMIONERO);
        registrarCargaCamionero(transportista.getId(), TipoMaterial.PANDERETA, 100.0);

        String body = """
            {
              "clienteNombre": "Cliente Uno",
              "clienteTelefono": "999111222",
              "direccionEnvio": "Av. Prueba 123",
              "tipoTransporte": "CAMIONERO",
              "material": "PANDERETA",
              "cantidad": 30.0,
              "montoTotal": 300.0,
              "adelanto": 100.0,
              "piso": 1,
              "horaEnvio": "%s",
              "transportistaId": %d
            }
            """.formatted(LocalDateTime.now().plusHours(2), transportista.getId());

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.clienteNombre").value("Cliente Uno"))
                .andExpect(jsonPath("$.tipoTransporte").value("CAMIONERO"))
                .andExpect(jsonPath("$.material").value("PANDERETA"))
                .andExpect(jsonPath("$.cantidad").value(30.0))
                .andExpect(jsonPath("$.codigoVerificacion").value("1234"));
    }

    // ========== PRUEBA 2 - CREAR PEDIDO VOLQUETERO ==========

    @Test
    @Order(2)
    @DisplayName("POST /api/pedidos - Debe crear pedido volquetero sin carga")
    void crearPedido_DebeCrearPedidoVolqueteroSinCarga() throws Exception {
        Transportista transportista = crearTransportista("Victor", "Volquete", "23232323", TipoTransporte.VOLQUETERO);

        String body = """
            {
              "clienteNombre": "Cliente Volquete",
              "clienteTelefono": "999333444",
              "direccionEnvio": "Jr. Volquete 456",
              "tipoTransporte": "VOLQUETERO",
              "material": "ARENA_GRUESA",
              "cantidad": 10.0,
              "montoTotal": 500.0,
              "adelanto": 200.0,
              "piso": 1,
              "horaEnvio": "%s",
              "transportistaId": %d
            }
            """.formatted(LocalDateTime.now().plusHours(3), transportista.getId());

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteNombre").value("Cliente Volquete"))
                .andExpect(jsonPath("$.tipoTransporte").value("VOLQUETERO"))
                .andExpect(jsonPath("$.material").value("ARENA_GRUESA"))
                .andExpect(jsonPath("$.codigoVerificacion").value("1234"));
    }

    // ========== PRUEBA 3 - LISTAR PEDIDOS ==========

    @Test
    @Order(3)
    @DisplayName("GET /api/pedidos - Debe retornar lista de pedidos")
    void listar_DebeRetornarPedidos() throws Exception {
        mockMvc.perform(get("/api/pedidos")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    // ========== PRUEBA 4 - LISTAR MIS PEDIDOS (VOLQUETERO - sin carga) ==========

    @Test
    @Order(4)
    @DisplayName("GET /api/pedidos/me - Debe retornar pedidos del transportista autenticado")
    void listarMisPedidos_DebeRetornarPedidosDelTransportista() throws Exception {
        // Crear transportista VOLQUETERO (no necesita carga)
        Transportista transportista = crearTransportista("Luis", "MisPedidos", "77777777", TipoTransporte.VOLQUETERO);

        // Login como transportista
        String loginBody = """
            {
              "username": "luis.mispedidos",
              "password": "77777777"
            }
            """;

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String transportistaToken = loginResponse.split("\"token\":\"")[1].split("\"")[0];

        // Crear pedido para este transportista (VOLQUETERO)
        String crearPedidoBody = """
            {
              "clienteNombre": "Cliente Transportista",
              "clienteTelefono": "999000111",
              "direccionEnvio": "Av. Transportista 789",
              "tipoTransporte": "VOLQUETERO",
              "material": "ARENA_FINA",
              "cantidad": 5.0,
              "montoTotal": 250.0,
              "adelanto": 50.0,
              "piso": 2,
              "horaEnvio": "%s",
              "transportistaId": %d
            }
            """.formatted(LocalDateTime.now().plusHours(4), transportista.getId());

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearPedidoBody))
                .andExpect(status().isOk());

        // Verificar que el transportista ve sus pedidos
        mockMvc.perform(get("/api/pedidos/me")
                        .header("Authorization", "Bearer " + transportistaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$[0].transportistaNombre").value("Luis MisPedidos"));
    }

    // ========== PRUEBA 5 - ERROR TIPO TRANSPORTE NO COINCIDE ==========
    @Test
    @Order(5)
    @DisplayName("POST /api/pedidos - Debe fallar cuando tipo de transporte no coincide")
    void crearPedido_DebeFallarCuandoTipoTransporteNoCoincide() throws Exception {
        Transportista transportista = crearTransportista("Tipo", "Incorrecto", "34343434", TipoTransporte.CAMIONERO);
        registrarCargaCamionero(transportista.getId(), TipoMaterial.PANDERETA, 100.0);

        String body = """
    {
      "clienteNombre": "Cliente Error",
      "clienteTelefono": "900000001",
      "direccionEnvio": "Direccion error",
      "tipoTransporte": "VOLQUETERO",
      "material": "PANDERETA",
      "cantidad": 10.0,
      "montoTotal": 100.0,
      "adelanto": 50.0,
      "piso": 1,
      "horaEnvio": "%s",
      "transportistaId": %d
    }
    """.formatted(LocalDateTime.now().plusHours(5), transportista.getId());

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    Exception resolvedException = result.getResolvedException();
                    assert resolvedException instanceof IllegalArgumentException;
                    assert resolvedException.getMessage().equals("El tipo de transporte del pedido no coincide con el transportista seleccionado");
                });
    }

// ========== PRUEBA 6 - ERROR CAMIONERO SIN CARGA ==========

    @Test
    @Order(6)
    @DisplayName("POST /api/pedidos - Debe fallar cuando camionero no tiene carga registrada")
    void crearPedido_DebeFallarCuandoCamioneroNoTieneCarga() throws Exception {
        Transportista transportista = crearTransportista("Sin", "Carga", "45454545", TipoTransporte.CAMIONERO);
        // NO registrar carga

        String body = """
    {
      "clienteNombre": "Cliente Sin Carga",
      "clienteTelefono": "900000002",
      "direccionEnvio": "Direccion sin carga",
      "tipoTransporte": "CAMIONERO",
      "material": "PANDERETA",
      "cantidad": 10.0,
      "montoTotal": 100.0,
      "adelanto": 50.0,
      "piso": 1,
      "horaEnvio": "%s",
      "transportistaId": %d
    }
    """.formatted(LocalDateTime.now().plusHours(6), transportista.getId());

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    Exception resolvedException = result.getResolvedException();
                    assert resolvedException instanceof RuntimeException;
                    assert resolvedException.getMessage().equals("El transportista no tiene carga registrada");
                });
    }

// ========== PRUEBA 7 - ERROR STOCK INSUFICIENTE ==========

    @Test
    @Order(7)
    @DisplayName("POST /api/pedidos - Debe fallar cuando el stock es insuficiente")
    void crearPedido_DebeFallarCuandoStockEsInsuficiente() throws Exception {
        Transportista transportista = crearTransportista("Stock", "Insuficiente", "56565656", TipoTransporte.CAMIONERO);
        registrarCargaCamionero(transportista.getId(), TipoMaterial.TECHO, 5.0);

        String body = """
    {
      "clienteNombre": "Cliente Stock",
      "clienteTelefono": "900000003",
      "direccionEnvio": "Direccion stock",
      "tipoTransporte": "CAMIONERO",
      "material": "TECHO",
      "cantidad": 10.0,
      "montoTotal": 100.0,
      "adelanto": 50.0,
      "piso": 1,
      "horaEnvio": "%s",
      "transportistaId": %d
    }
    """.formatted(LocalDateTime.now().plusHours(7), transportista.getId());

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    Exception resolvedException = result.getResolvedException();
                    assert resolvedException instanceof IllegalArgumentException;
                    assert resolvedException.getMessage().equals("Stock insuficiente para atender el pedido");
                });
    }

// ========== PRUEBA 8 - ERROR MATERIAL CAMIONERO INVALIDO ==========

    @Test
    @Order(8)
    @DisplayName("POST /api/pedidos - Debe fallar cuando camionero usa material no permitido")
    void crearPedido_DebeFallarCuandoCamioneroUsaMaterialInvalido() throws Exception {
        Transportista transportista = crearTransportista("Material", "Invalido", "99999999", TipoTransporte.CAMIONERO);

        // No registrar carga porque el material es inválido
        String body = """
    {
      "clienteNombre": "Cliente Material Invalido",
      "clienteTelefono": "900000004",
      "direccionEnvio": "Direccion invalida",
      "tipoTransporte": "CAMIONERO",
      "material": "ARENA_FINA",
      "cantidad": 10.0,
      "montoTotal": 100.0,
      "adelanto": 50.0,
      "piso": 1,
      "horaEnvio": "%s",
      "transportistaId": %d
    }
    """.formatted(LocalDateTime.now().plusHours(8), transportista.getId());

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    Exception resolvedException = result.getResolvedException();
                    assert resolvedException instanceof IllegalArgumentException;
                    assert resolvedException.getMessage().equals("El transportista camionero solo puede trabajar con materiales PANDERETA o TECHO");
                });
    }

    // ========== PRUEBA 9 - SIN AUTENTICACION ==========

    @Test
    @Order(9)
    @DisplayName("POST /api/pedidos - Debe fallar cuando no hay autenticación")
    void crearPedido_DebeFallarCuandoNoHayAutenticacion() throws Exception {
        String body = """
            {
              "clienteNombre": "Cliente No Auth",
              "clienteTelefono": "900000005",
              "direccionEnvio": "Direccion no auth",
              "tipoTransporte": "CAMIONERO",
              "material": "PANDERETA",
              "cantidad": 10.0,
              "montoTotal": 100.0,
              "adelanto": 50.0,
              "piso": 1,
              "horaEnvio": "%s",
              "transportistaId": 1
            }
            """.formatted(LocalDateTime.now().plusHours(9));

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}