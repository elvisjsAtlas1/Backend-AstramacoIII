package com.example.backendastramaco.security.config;

import com.example.backendastramaco.security.jwt.JwtFilter;
import com.example.backendastramaco.security.service.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigUnitTest {

    @Mock
    private JwtFilter jwtFilter;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    @DisplayName("Debe crear correctamente el AuthenticationProvider")
    void debeCrearAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = securityConfig.authenticationProvider();
        assertNotNull(authProvider, "El AuthenticationProvider no debería ser nulo");
    }

    @Test
    @DisplayName("Debe crear correctamente el PasswordEncoder")
    void debeCrearPasswordEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        assertNotNull(encoder, "El PasswordEncoder no debería ser nulo");
        assertTrue(encoder instanceof BCryptPasswordEncoder);
    }

    @Test
    @DisplayName("Debe crear correctamente la configuración CORS")
    void debeCrearCorsConfigurationSource() {
        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();
        assertNotNull(corsSource, "La configuración CORS no debería ser nula");
        // No intentamos obtener la configuración porque requiere un request HTTP real
    }

    @Test
    @DisplayName("Debe crear correctamente el AuthenticationManager")
    void debeCrearAuthenticationManager() throws Exception {
        AuthenticationConfiguration authConfig = mock(AuthenticationConfiguration.class);
        AuthenticationManager expectedManager = mock(AuthenticationManager.class);
        when(authConfig.getAuthenticationManager()).thenReturn(expectedManager);

        AuthenticationManager result = securityConfig.authenticationManager(authConfig);

        assertNotNull(result);
        assertEquals(expectedManager, result);
        verify(authConfig, times(1)).getAuthenticationManager();
    }
}