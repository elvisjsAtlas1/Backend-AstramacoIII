package com.example.backendastramaco.integration;

import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthIntegrationTest extends AuthBaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private static final String LOGIN_URL = "/api/auth/login";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    @Test
    @DisplayName("Debe retornar token cuando las credenciales son correctas")
    void login_DebeRetornarToken_CuandoCredencialesSonCorrectas() throws Exception {
        // Arrange
        String requestBody = buildLoginRequestBody(ADMIN_USERNAME, ADMIN_PASSWORD);

        // Act
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    @DisplayName("Debe retornar 401 Unauthorized cuando la contraseña es incorrecta")
    void login_DebeRetornarUnauthorized_CuandoPasswordEsIncorrecto() throws Exception {
        // Arrange
        String requestBody = buildLoginRequestBody(ADMIN_USERNAME, "passwordIncorrecto");

        // Act
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // Assert
        result.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Debe retornar 401 Unauthorized cuando el usuario no existe")
    void login_DebeRetornarUnauthorized_CuandoUsuarioNoExiste() throws Exception {
        // Arrange
        String requestBody = buildLoginRequestBody("usuario_no_existe", ADMIN_PASSWORD);

        // Act
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // Assert
        result.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Debe crear usuario admin inicial en base de datos de prueba")
    void debeCrearUsuarioAdminInicialEnBaseDeDatosDePrueba() {
        // Act
        var adminOptional = usuarioRepository.findByUsername(ADMIN_USERNAME);

        // Assert - Usando JUnit assertions correctas
        assertTrue(adminOptional.isPresent(), "El usuario admin debería existir en la base de datos");

        Usuario admin = adminOptional.get();
        assertEquals("ADMIN", admin.getRol().name(), "El rol del admin debería ser ADMIN");
        assertTrue(admin.getActivo(), "El admin debería estar activo");
    }

    @Test
    @DisplayName("Debe retornar 401 Unauthorized cuando el request body está vacío")
    void login_DebeRetornarUnauthorized_CuandoRequestBodyEstaVacio() throws Exception {
        // Arrange
        String emptyBody = "{}";

        // Act
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyBody));

        // Assert
        result.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Debe retornar 400 Bad Request cuando el request body es inválido")
    void login_DebeRetornarBadRequest_CuandoRequestBodyEsInvalido() throws Exception {
        // Arrange
        String invalidBody = "{username: admin}"; // JSON inválido

        // Act
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody));

        // Assert
        result.andExpect(status().isBadRequest());
    }

    private String buildLoginRequestBody(String username, String password) {
        return String.format("""
                {
                  "username": "%s",
                  "password": "%s"
                }
                """, username, password);
    }
}