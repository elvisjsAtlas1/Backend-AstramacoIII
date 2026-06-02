package com.example.backendastramaco.controller;

import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.security.dto.AuthRequest;
import com.example.backendastramaco.security.dto.AuthResponse;
import com.example.backendastramaco.security.jwt.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "API para autenticación y gestión de tokens JWT")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica a un usuario y retorna un token JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login exitoso - Token generado"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas", content = @Content)
    })
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        //                                   ↑↑↑↑↑
        //                          IMPORTANTE: Añadir @Valid aquí

        log.info("Intento de login para usuario: {}", request.getUsername());

        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            if (authentication.isAuthenticated()) {
                String token = jwtUtil.generateToken(request.getUsername());
                Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado después de autenticación"));

                AuthResponse response = new AuthResponse(
                        token,
                        usuario.getUsername(),
                        usuario.getRol().name()
                );

                log.info("Login exitoso para usuario: {}", request.getUsername());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Autenticación fallida para usuario: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Credenciales inválidas"));
            }

        } catch (BadCredentialsException e) {
            log.warn("Contraseña incorrecta para usuario: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));

        } catch (DisabledException e) {
            log.warn("Usuario deshabilitado: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario deshabilitado"));

        } catch (LockedException e) {
            log.warn("Usuario bloqueado: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario bloqueado"));

        } catch (AuthenticationException e) {
            log.error("Error de autenticación para usuario {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));

        } catch (Exception e) {
            log.error("Error inesperado en login para usuario {}: {}", request.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }
}