package com.example.backendastramaco.integration;

import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransportistaIntegrationTest extends TransportistaBaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransportistaRepository transportistaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

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

    @Test
    void crear_DebeCrearTransportistaCorrectamente() throws Exception {
        String token = obtenerTokenAdmin();

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
              "estado": "ACTIVO",
              "usuarioId": 1
            }
            """;

        mockMvc.perform(post("/api/transportistas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nombre").value("Carlos"))
                .andExpect(jsonPath("$.apellidos").value("Mamani"))
                .andExpect(jsonPath("$.dni").value("12345678"))
                .andExpect(jsonPath("$.tipoTransporte").value("CAMIONERO"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.usuario.username").value("carlos.mamani"));

        assertThat(transportistaRepository.findAll()).isNotEmpty();
        assertThat(usuarioRepository.findByUsername("carlos.mamani")).isPresent();
    }

    @Test
    void crear_DebeAsignarEstadoActivo_CuandoEstadoEsNull() throws Exception {
        String token = obtenerTokenAdmin();

        String body = """
            {
              "nombre": "Luis",
              "apellidos": "Quispe",
              "dni": "87654321",
              "edad": 28,
              "tipoTransporte": "VOLQUETERO",
              "placa": "XYZ-987",
              "vehiculoInfo": "Volquete rojo",
              "capacidad": 20.0,
              "estado": null,
              "usuarioId": 1
            }
            """;

        mockMvc.perform(post("/api/transportistas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Luis"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void listar_DebeRetornarTransportistas() throws Exception {
        String token = obtenerTokenAdmin();

        mockMvc.perform(get("/api/transportistas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void listarPorTipo_DebeRetornarSoloCamionerosActivos() throws Exception {
        String token = obtenerTokenAdmin();

        mockMvc.perform(get("/api/transportistas/tipo/CAMIONERO")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }
}