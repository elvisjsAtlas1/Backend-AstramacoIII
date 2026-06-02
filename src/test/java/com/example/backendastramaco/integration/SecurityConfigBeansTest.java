package com.example.backendastramaco.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigBeansTest extends SecurityConfigBaseIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("SecurityFilterChain debe ser un bean válido")
    void securityFilterChain_DebeSerBeanValido() {
        SecurityFilterChain filterChain = applicationContext.getBean(SecurityFilterChain.class);
        assertNotNull(filterChain);
    }

    @Test
    @DisplayName("AuthenticationProvider debe ser un bean válido")
    void authenticationProvider_DebeSerBeanValido() {
        DaoAuthenticationProvider authProvider = applicationContext.getBean(DaoAuthenticationProvider.class);
        assertNotNull(authProvider);
    }

    @Test
    @DisplayName("PasswordEncoder debe ser un bean válido")
    void passwordEncoder_DebeSerBeanValido() {
        PasswordEncoder encoder = applicationContext.getBean(PasswordEncoder.class);
        assertNotNull(encoder);
    }

    @Test
    @DisplayName("AuthenticationManager debe ser un bean válido")
    void authenticationManager_DebeSerBeanValido() {
        AuthenticationManager authManager = applicationContext.getBean(AuthenticationManager.class);
        assertNotNull(authManager);
    }

    @Test
    @DisplayName("CorsConfigurationSource debe ser un bean válido")
    void corsConfigurationSource_DebeSerBeanValido() {
        CorsConfigurationSource corsSource = (CorsConfigurationSource) applicationContext.getBean("corsConfigurationSource");
        assertNotNull(corsSource);
    }

    @Test
    @DisplayName("PasswordEncoder debe codificar correctamente")
    void passwordEncoder_DebeCodificarCorrectamente() {
        PasswordEncoder encoder = applicationContext.getBean(PasswordEncoder.class);

        String rawPassword = "test123";
        String encodedPassword = encoder.encode(rawPassword);

        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encoder.matches(rawPassword, encodedPassword));
    }
}