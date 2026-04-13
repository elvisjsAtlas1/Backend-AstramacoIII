package com.example.backendastramaco.security.jwt;

import com.example.backendastramaco.security.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String header = request.getHeader(AUTHORIZATION_HEADER);
        final String requestUri = request.getRequestURI();

        log.debug("Procesando JWT para URI: {}", requestUri);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            log.debug("No se encontró header Bearer para la URI: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring(BEARER_PREFIX.length());
            String username = jwtUtil.extractUsername(token);

            log.debug("Username extraído del token para URI {}: {}", requestUri, username);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(token)) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    SecurityContextHolder.getContext().setAuthentication(auth);

                    log.debug("Usuario autenticado correctamente: {}", username);
                } else {
                    log.warn("Token JWT inválido para la URI: {}", requestUri);
                }
            }

        } catch (Exception e) {
            log.error("Error procesando JWT en la URI {}: {}", requestUri, e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}