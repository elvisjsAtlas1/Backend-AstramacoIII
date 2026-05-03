package com.example.backendastramaco.integration;

import com.example.backendastramaco.dto.TransportistaRequestDTO;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.DocumentoPersonalRepository;
import com.example.backendastramaco.service.TransportistaService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentoPersonalIntegrationTest extends DocumentoPersonalBaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransportistaService transportistaService;

    @Autowired
    private DocumentoPersonalRepository documentoPersonalRepository;

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
        dto.setPlaca("DOC-" + dni.substring(0, 3));
        dto.setVehiculoInfo("Vehiculo de prueba");
        dto.setCapacidad(15.0);
        dto.setEstado("ACTIVO");
        dto.setUsuarioId(1L);

        return transportistaService.crear(dto);
    }

    @Test
    void guardar_DebeRegistrarDocumentoSoatCorrectamente() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Marco", "Apaza", "11112222", TipoTransporte.CAMIONERO);

        String body = """
            {
              "tipoDocumento": "SOAT",
              "valor": "SOAT-2026",
              "fechaVencimiento": "%s"
            }
            """.formatted(LocalDate.now().plusYears(1));

        mockMvc.perform(post("/api/documentos/" + transportista.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.tipoDocumento").value("SOAT"))
                .andExpect(jsonPath("$.valor").value("SOAT-2026"))
                .andExpect(jsonPath("$.activo").value(true));

        assertThat(documentoPersonalRepository.findByTransportistaId(transportista.getId())).hasSize(1);
    }

    @Test
    void guardar_DebeRegistrarLicenciaConValorSi() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Pedro", "Condori", "33334444", TipoTransporte.VOLQUETERO);

        String body = """
            {
              "tipoDocumento": "LICENCIA",
              "valor": "SI",
              "fechaVencimiento": null
            }
            """;

        mockMvc.perform(post("/api/documentos/" + transportista.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDocumento").value("LICENCIA"))
                .andExpect(jsonPath("$.valor").value("SI"));
    }

    @Test
    void listar_DebeRetornarDocumentosDeTransportista() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Jose", "Mamani", "55556666", TipoTransporte.CAMIONERO);

        String body = """
            {
              "tipoDocumento": "DNI",
              "valor": "55556666",
              "fechaVencimiento": null
            }
            """;

        mockMvc.perform(post("/api/documentos/" + transportista.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/documentos/transportista/" + transportista.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$[0].tipoDocumento").value("DNI"));
    }

    @Test
    void guardar_DebeRetornarError_CuandoSoatNoTieneFecha() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Ruben", "Flores", "77778888", TipoTransporte.CAMIONERO);

        String body = """
        {
          "tipoDocumento": "SOAT",
          "valor": "SOAT-SIN-FECHA",
          "fechaVencimiento": null
        }
        """;

        assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/api/documentos/" + transportista.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
        );
    }

    @Test
    void guardar_DebeRetornarError_CuandoDocumentoDuplicado() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Alberto", "Ramos", "99990000", TipoTransporte.CAMIONERO);

        String body = """
        {
          "tipoDocumento": "DNI",
          "valor": "99990000",
          "fechaVencimiento": null
        }
        """;

        mockMvc.perform(post("/api/documentos/" + transportista.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/api/documentos/" + transportista.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
        );
    }
}