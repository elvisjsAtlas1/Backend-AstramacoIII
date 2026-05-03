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
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    private void registrarCarga(Long transportistaId, TipoMaterial material, Double cantidad) {
        CargaRequestDTO dto = new CargaRequestDTO();
        dto.setTipoMaterial(material);
        dto.setCantidadDisponible(cantidad);
        cargaService.subirCargaActual(transportistaId, dto);
    }

    @Test
    void crearPedido_DebeCrearPedidoCamioneroYDescontarCarga() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Pedro", "Pedido", "12121212", TipoTransporte.CAMIONERO);
        registrarCarga(transportista.getId(), TipoMaterial.PANDERETA, 100.0);

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
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.clienteNombre").value("Cliente Uno"))
                .andExpect(jsonPath("$.tipoTransporte").value("CAMIONERO"))
                .andExpect(jsonPath("$.material").value("PANDERETA"))
                .andExpect(jsonPath("$.cantidad").value(30.0))
                .andExpect(jsonPath("$.estado").value("EN_ENVIO"))
                .andExpect(jsonPath("$.codigoVerificacion").value("1234"));

        var carga = cargaRepository.findByTransportistaId(transportista.getId());
        assertThat(carga).isPresent();
        assertThat(carga.get().getCantidadDisponible()).isEqualTo(70.0);
    }

    @Test
    void crearPedido_DebeCrearPedidoVolqueteroSinCarga() throws Exception {
        String token = obtenerTokenAdmin();
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
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteNombre").value("Cliente Volquete"))
                .andExpect(jsonPath("$.tipoTransporte").value("VOLQUETERO"))
                .andExpect(jsonPath("$.material").value("ARENA_GRUESA"))
                .andExpect(jsonPath("$.codigoVerificacion").value("1234"));

        assertThat(pedidoRepository.findAll()).isNotEmpty();
    }

    @Test
    void listar_DebeRetornarPedidos() throws Exception {
        String token = obtenerTokenAdmin();

        mockMvc.perform(get("/api/pedidos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void crearPedido_DebeFallarCuandoTipoTransporteNoCoincide() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Tipo", "Incorrecto", "34343434", TipoTransporte.CAMIONERO);
        registrarCarga(transportista.getId(), TipoMaterial.PANDERETA, 100.0);

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
            """.formatted(LocalDateTime.now().plusHours(4), transportista.getId());

        assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
        );
    }

    @Test
    void crearPedido_DebeFallarCuandoCamioneroNoTieneCarga() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Sin", "Carga", "45454545", TipoTransporte.CAMIONERO);

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
            """.formatted(LocalDateTime.now().plusHours(5), transportista.getId());

        assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
        );
    }

    @Test
    void crearPedido_DebeFallarCuandoStockEsInsuficiente() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Stock", "Insuficiente", "56565656", TipoTransporte.CAMIONERO);
        registrarCarga(transportista.getId(), TipoMaterial.TECHO, 5.0);

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
            """.formatted(LocalDateTime.now().plusHours(6), transportista.getId());

        assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
        );
    }
}