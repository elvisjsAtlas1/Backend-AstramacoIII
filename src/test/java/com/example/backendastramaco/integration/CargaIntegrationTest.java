package com.example.backendastramaco.integration;

import com.example.backendastramaco.dto.TransportistaRequestDTO;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.CargaRepository;
import com.example.backendastramaco.service.TransportistaService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CargaIntegrationTest extends CargaBaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransportistaService transportistaService;

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
        dto.setPlaca("CAR-" + dni.substring(0, 3));
        dto.setVehiculoInfo("Vehiculo carga test");
        dto.setCapacidad(20.0);
        dto.setEstado("ACTIVO");
        dto.setUsuarioId(1L);

        return transportistaService.crear(dto);
    }

    @Test
    void subirCargaActual_DebeRegistrarCargaParaCamionero() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Carga", "Camionero", "10101010", TipoTransporte.CAMIONERO);

        String body = """
            {
              "tipoMaterial": "PANDERETA",
              "cantidadDisponible": 100.0
            }
            """;

        mockMvc.perform(put("/api/cargas/" + transportista.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.transportistaId").value(transportista.getId()))
                .andExpect(jsonPath("$.transportistaNombre").value("Carga Camionero"))
                .andExpect(jsonPath("$.tipoMaterial").value("PANDERETA"))
                .andExpect(jsonPath("$.cantidadDisponible").value(100.0));

        assertThat(cargaRepository.findByTransportistaId(transportista.getId())).isPresent();
    }

    @Test
    void aumentarCargaActual_DebeSumarCantidadCuandoMaterialEsIgual() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Aumentar", "Carga", "20202020", TipoTransporte.CAMIONERO);

        String cargaInicial = """
            {
              "tipoMaterial": "TECHO",
              "cantidadDisponible": 50.0
            }
            """;

        mockMvc.perform(put("/api/cargas/" + transportista.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cargaInicial))
                .andExpect(status().isOk());

        String aumentar = """
            {
              "tipoMaterial": "TECHO",
              "cantidadAgregar": 25.0
            }
            """;

        mockMvc.perform(post("/api/cargas/" + transportista.getId() + "/aumentar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aumentar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoMaterial").value("TECHO"))
                .andExpect(jsonPath("$.cantidadDisponible").value(75.0));
    }

    @Test
    void obtenerCarga_DebeRetornarCargaDelTransportista() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Obtener", "Carga", "30303030", TipoTransporte.CAMIONERO);

        String body = """
            {
              "tipoMaterial": "PANDERETA",
              "cantidadDisponible": 80.0
            }
            """;

        mockMvc.perform(put("/api/cargas/" + transportista.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cargas/" + transportista.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transportistaId").value(transportista.getId()))
                .andExpect(jsonPath("$.tipoMaterial").value("PANDERETA"))
                .andExpect(jsonPath("$.cantidadDisponible").value(80.0));
    }

    @Test
    void listarTodas_DebeRetornarListaDeCargas() throws Exception {
        String token = obtenerTokenAdmin();

        mockMvc.perform(get("/api/cargas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void subirCargaActual_DebeFallarCuandoTransportistaEsVolquetero() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Volquetero", "SinCarga", "40404040", TipoTransporte.VOLQUETERO);

        String body = """
            {
              "tipoMaterial": "PANDERETA",
              "cantidadDisponible": 100.0
            }
            """;

        assertThrows(ServletException.class, () ->
                mockMvc.perform(put("/api/cargas/" + transportista.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
        );
    }

    @Test
    void subirCargaActual_DebeFallarCuandoMaterialNoEsPermitidoParaCamionero() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Material", "Invalido", "50505050", TipoTransporte.CAMIONERO);

        String body = """
            {
              "tipoMaterial": "ARENA_GRUESA",
              "cantidadDisponible": 100.0
            }
            """;

        assertThrows(ServletException.class, () ->
                mockMvc.perform(put("/api/cargas/" + transportista.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
        );
    }

    @Test
    void aumentarCargaActual_DebeFallarCuandoMaterialEsDistinto() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Material", "Distinto", "60606060", TipoTransporte.CAMIONERO);

        String cargaInicial = """
            {
              "tipoMaterial": "PANDERETA",
              "cantidadDisponible": 40.0
            }
            """;

        mockMvc.perform(put("/api/cargas/" + transportista.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cargaInicial))
                .andExpect(status().isOk());

        String aumentar = """
            {
              "tipoMaterial": "TECHO",
              "cantidadAgregar": 10.0
            }
            """;

        assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/api/cargas/" + transportista.getId() + "/aumentar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aumentar))
        );
    }
}