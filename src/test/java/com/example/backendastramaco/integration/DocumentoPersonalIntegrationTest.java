package com.example.backendastramaco.integration;

import com.example.backendastramaco.dto.TransportistaRequestDTO;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.DocumentoPersonalRepository;
import com.example.backendastramaco.service.TransportistaService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentoPersonalIntegrationTest extends DocumentoPersonalBaseIntegrationTest {

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

        return response.split(TOKEN_SPLIT_REGEX)[1].split(TOKEN_SPLIT_END)[0];
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
    @DisplayName("Debe registrar un documento SOAT correctamente con todas sus fechas obligatorias")
    void guardar_DebeRegistrarDocumentoSoatCorrectamente() throws Exception {
        String token = obtenerTokenAdmin();
        Transportista transportista = crearTransportista("Marco", "Apaza", "11112222", TipoTransporte.CAMIONERO);

        String body = """
        {
          "tipoDocumento": "SOAT",
          "valor": "SOAT-2026",
          "fechaEmision": "%s",
          "fechaVencimiento": "%s"
        }
        """.formatted(LocalDate.now(), LocalDate.now().plusYears(1));

        mockMvc.perform(post("/api/documentos/" + transportista.getId())
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.tipoDocumento").value("SOAT"))
                .andExpect(jsonPath("$.valor").value("SOAT-2026"))
                .andExpect(jsonPath("$.fechaEmision").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.activo").value(true));

        assertThat(documentoPersonalRepository.findByTransportistaId(transportista.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Debe registrar licencia con valor SI")
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
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDocumento").value("LICENCIA"))
                .andExpect(jsonPath("$.valor").value("SI"));
    }

    @Test
    @DisplayName("Debe listar documentos del transportista")
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
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/documentos/transportista/" + transportista.getId())
                        .header(AUTHORIZATION, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$[0].tipoDocumento").value("DNI"));
    }

    @Test
    @DisplayName("Debe retornar error cuando SOAT no tiene fecha")
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

        // ✅ CORRECCIÓN: Verificar el status en lugar de assertThrows
        mockMvc.perform(post("/api/documentos/" + transportista.getId())
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("SOAT y REVISIÓN TÉCNICA requieren obligatoriamente fecha de emisión y vencimiento"));
    }

    @Test
    @DisplayName("Debe retornar error cuando el documento está duplicado")
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

        // Primer registro - debe funcionar
        mockMvc.perform(post("/api/documentos/" + transportista.getId())
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Segundo registro con el mismo tipo de documento - debe fallar
        // ✅ CORRECCIÓN: Verificar el status en lugar de assertThrows
        mockMvc.perform(post("/api/documentos/" + transportista.getId())
                        .header(AUTHORIZATION, BEARER_PREFIX + token)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El documento tipo DNI ya está registrado"));
    }

}