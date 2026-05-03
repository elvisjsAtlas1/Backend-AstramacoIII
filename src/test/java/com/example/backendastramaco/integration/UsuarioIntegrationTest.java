package com.example.backendastramaco.integration;

import com.example.backendastramaco.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UsuarioIntegrationTest extends UsuarioBaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void crear_DebeCrearUsuarioCorrectamente() throws Exception {
        String token = obtenerTokenAdmin();

        String body = """
        {
          "username": "usuario.integration",
          "password": "123456",
          "rol": "USER"
        }
        """;

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void crear_DebeGuardarPasswordEncriptado() throws Exception {
        String token = obtenerTokenAdmin();
        String body = """
            {
              "username": "usuario.password",
              "password": "miPassword123",
              "rol": "USER"
            }
            """;

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void crear_DebeCrearUsuarioTransportista() throws Exception {
        String token = obtenerTokenAdmin();
        String body = """
            {
              "username": "transportista.integration",
              "password": "123456",
              "rol": "TRANSPORTISTA"
            }
            """;

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}