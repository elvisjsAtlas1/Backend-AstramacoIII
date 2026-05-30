package com.example.backendastramaco.security.config;

import com.example.backendastramaco.integration.UsuarioBaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// 🔥 Heredamos de la clase base de integración para reutilizar el contenedor MySQL de Docker
class SecurityConfigUnitTest extends UsuarioBaseIntegrationTest {

    @Autowired(required = false)
    private SecurityFilterChain securityFilterChain;

    @Autowired(required = false)
    private DaoAuthenticationProvider authenticationProvider;

    @Autowired(required = false)
    private AuthenticationManager authenticationManager;

    @Autowired(required = false)
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Debe verificar la carga y existencia de los Beans de Seguridad para cobertura JaCoCo")
    void verificarBeansDeSeguridad_DebeEstarCargadosEnElContexto() {
        assertNotNull(securityFilterChain, "El SecurityFilterChain debería estar inicializado");
        assertNotNull(authenticationProvider, "El DaoAuthenticationProvider debería estar inicializado");
        assertNotNull(authenticationManager, "El AuthenticationManager debería estar inicializado");
        assertNotNull(corsConfigurationSource, "El CorsConfigurationSource debería estar inicializado");
        assertNotNull(passwordEncoder, "El PasswordEncoder debería estar inicializado");
    }
}