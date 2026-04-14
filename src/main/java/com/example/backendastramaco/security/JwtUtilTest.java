package com.example.backendastramaco.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        ReflectionTestUtils.setField(
                jwtUtil,
                "jwtSecret",
                "ClaveSuperSeguraJWT2026_ClaveMinima32Bytes"
        );
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 86400000L);

        jwtUtil.init();
    }

    @Test
    @DisplayName("Debe generar un token válido")
    void generateToken_DebeGenerarToken() {
        String token = jwtUtil.generateToken("admin");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("Debe extraer correctamente el username desde el token")
    void extractUsername_DebeExtraerUsername() {
        String token = jwtUtil.generateToken("admin");

        String username = jwtUtil.extractUsername(token);

        assertEquals("admin", username);
    }

    @Test
    @DisplayName("Debe validar correctamente un token válido")
    void validateToken_DebeRetornarTrueParaTokenValido() {
        String token = jwtUtil.generateToken("admin");

        boolean valido = jwtUtil.validateToken(token);

        assertTrue(valido);
    }

    @Test
    @DisplayName("Debe retornar false cuando el token es inválido")
    void validateToken_DebeRetornarFalseParaTokenInvalido() {
        boolean valido = jwtUtil.validateToken("token-falso");

        assertFalse(valido);
    }

    @Test
    @DisplayName("Debe retornar false cuando el token está vacío")
    void validateToken_DebeRetornarFalseParaTokenVacio() {
        boolean valido = jwtUtil.validateToken("");

        assertFalse(valido);
    }
}