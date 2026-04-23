package com.example.backendastramaco;

import com.example.backendastramaco.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtUtilTests {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    @DisplayName("Debe generar un token válido")
    void generateToken_DebeGenerarTokenValido() {
        String username = "juan.perez";

        String token = jwtUtil.generateToken(username);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("Debe extraer el username correctamente desde el token")
    void extractUsername_DebeRetornarUsernameCorrecto() {
        String username = "juan.perez";
        String token = jwtUtil.generateToken(username);

        String resultado = jwtUtil.extractUsername(token);

        assertEquals(username, resultado);
    }

    @Test
    @DisplayName("Debe validar correctamente un token válido")
    void validateToken_DebeRetornarTrueCuandoTokenEsValido() {
        String token = jwtUtil.generateToken("juan.perez");

        boolean resultado = jwtUtil.validateToken(token);

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Debe retornar false cuando el token es inválido")
    void validateToken_DebeRetornarFalseCuandoTokenEsInvalido() {
        String tokenInvalido = "token.invalido.aqui";

        boolean resultado = jwtUtil.validateToken(tokenInvalido);

        assertFalse(resultado);
    }
}