package com.example.backendastramaco.unitarias;

import com.example.backendastramaco.security.config.SecurityConfig;
import com.example.backendastramaco.security.jwt.JwtFilter;
import com.example.backendastramaco.security.service.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("Debe crear el SecurityFilterChain con la configuración correcta")
    void filterChain_DebeConfigurarSeguridadCorrectamente() throws Exception {
        // Arrange
        HttpSecurity http = mock(HttpSecurity.class);

        when(http.cors(any())).thenReturn(http);
        when(http.csrf(any())).thenReturn(http);
        when(http.sessionManagement(any())).thenReturn(http);
        when(http.exceptionHandling(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.authenticationProvider(any())).thenReturn(http);
        when(http.addFilterBefore(any(), any())).thenReturn(http);
        when(http.build()).thenReturn(mock(DefaultSecurityFilterChain.class));

        // Act
        SecurityFilterChain result = securityConfig.filterChain(http);

        // Assert
        assertNotNull(result);
        verify(http, atLeastOnce()).cors(any());
        verify(http, atLeastOnce()).csrf(any());
        verify(http, atLeastOnce()).sessionManagement(any());
        verify(http, atLeastOnce()).exceptionHandling(any());
        verify(http, atLeastOnce()).authorizeHttpRequests(any());
        verify(http, atLeastOnce()).authenticationProvider(any());
        verify(http, atLeastOnce()).addFilterBefore(eq(jwtFilter), eq(UsernamePasswordAuthenticationFilter.class));
        verify(http, atLeastOnce()).build();
    }
}