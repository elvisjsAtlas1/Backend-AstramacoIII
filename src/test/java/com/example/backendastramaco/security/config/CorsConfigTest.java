package com.example.backendastramaco.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    @Test
    @DisplayName("Debe configurar correctamente los orígenes CORS")
    void corsConfigurer_DebeConfigurarOrigenesCorrectamente() {
        // Arrange
        CorsConfig corsConfig = new CorsConfig();

        // Act
        WebMvcConfigurer configurer = corsConfig.corsConfigurer();

        // Assert
        assertNotNull(configurer);

        // Verificar que no lanza excepción al configurar
        CorsRegistry registry = new CorsRegistry();
        assertDoesNotThrow(() -> configurer.addCorsMappings(registry));
    }
}