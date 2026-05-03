package com.example.backendastramaco.integration;

import com.example.backendastramaco.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class AuthIntegrationTest extends AuthBaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void login_DebeRetornarToken_CuandoCredencialesSonCorrectas() throws Exception {
        String body = """
            {
              "username": "admin",
              "password": "admin123"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void login_DebeRetornarForbidden_CuandoPasswordEsIncorrecto() throws Exception {
        String body = """
        {
          "username": "admin",
          "password": "passwordIncorrecto"
        }
        """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_DebeRetornarForbidden_CuandoUsuarioNoExiste() throws Exception {
        String body = """
        {
          "username": "usuario_no_existe",
          "password": "admin123"
        }
        """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void debeCrearUsuarioAdminInicialEnBaseDeDatosDePrueba() {
        var admin = usuarioRepository.findByUsername("admin");

        assert admin.isPresent();
        assert admin.get().getRol().name().equals("ADMIN");
        assert admin.get().getActivo();
    }
}