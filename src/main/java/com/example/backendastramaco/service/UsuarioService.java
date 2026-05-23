package com.example.backendastramaco.service;

import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.service.audit.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Transactional
    public Usuario crear(Usuario usuario) {
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setActivo(true);

        Usuario saved = usuarioRepository.save(usuario);

        // Auditar creación
        auditService.auditUsuario(
                saved.getId(),
                "CREATE",
                null,
                saved,
                request
        );

        return saved;
    }

    @Transactional
    public Usuario actualizar(Long id, Usuario nuevosDatos) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Guardar copia del estado anterior
        Usuario oldCopy = copiarEntidad(existente);

        // Actualizar campos
        existente.setUsername(nuevosDatos.getUsername());
        if (nuevosDatos.getPassword() != null && !nuevosDatos.getPassword().isEmpty()) {
            existente.setPassword(passwordEncoder.encode(nuevosDatos.getPassword()));
        }
        existente.setRol(nuevosDatos.getRol());
        existente.setActivo(nuevosDatos.getActivo());

        Usuario updated = usuarioRepository.save(existente);

        // Auditar actualización
        auditService.auditUsuario(
                id,
                "UPDATE",
                oldCopy,
                updated,
                request
        );

        return updated;
    }

    @Transactional
    public void eliminar(Long id, String username) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Guardar copia para auditoría
        Usuario oldCopy = copiarEntidad(existente);

        // Soft delete
        existente.softDelete(username);
        usuarioRepository.save(existente);

        // Auditar eliminación
        auditService.auditUsuario(
                id,
                "DELETE",
                oldCopy,
                null,
                request
        );
    }

    private Usuario copiarEntidad(Usuario original) {
        Usuario copia = new Usuario();
        copia.setId(original.getId());
        copia.setUsername(original.getUsername());
        copia.setPassword(original.getPassword());
        copia.setRol(original.getRol());
        copia.setActivo(original.getActivo());
        return copia;
    }
}