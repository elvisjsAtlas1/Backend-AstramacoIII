package com.example.backendastramaco.service;

import com.example.backendastramaco.dto.TransportistaRequestDTO;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.enums.EstadoTransportista;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.service.audit.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TransportistaService {

    private final TransportistaRepository transportistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Transactional
    public Transportista crear(TransportistaRequestDTO dto) {

        Transportista t = new Transportista();

        Usuario usuario = new Usuario();

        String username = generarUsernameUnico(dto.getNombre(), dto.getApellidos());

        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(dto.getDni()));
        usuario.setRol(Rol.TRANSPORTISTA);
        usuario.setActivo(true);

        usuario = usuarioRepository.save(usuario);

        t.setUsuario(usuario);
        t.setNombre(dto.getNombre());
        t.setApellidos(dto.getApellidos());
        t.setDni(dto.getDni());
        t.setEdad(dto.getEdad());
        t.setTipoTransporte(dto.getTipoTransporte());
        t.setPlaca(dto.getPlaca());
        t.setVehiculoInfo(dto.getVehiculoInfo());
        t.setCapacidad(dto.getCapacidad());

        if (dto.getEstado() == null || dto.getEstado().isBlank()) {
            t.setEstado(EstadoTransportista.ACTIVO);
        } else {
            t.setEstado(EstadoTransportista.valueOf(dto.getEstado().toUpperCase(Locale.ROOT)));
        }

        Transportista saved = transportistaRepository.save(t);

        // Auditar creación
        auditService.auditTransportista(
                saved.getId(),
                "CREATE",
                null,
                saved,
                request
        );

        return saved;
    }

    private String generarUsername(String nombre, String apellidos) {
        String primerNombre = nombre.split(" ")[0].toLowerCase(Locale.ROOT);
        String primerApellido = apellidos.split(" ")[0].toLowerCase(Locale.ROOT);
        return primerNombre + "." + primerApellido;
    }

    private String generarUsernameUnico(String nombre, String apellidos) {
        String base = generarUsername(nombre, apellidos);
        String username = base;
        int contador = 1;

        while (usuarioRepository.findByUsername(username).isPresent()) {
            username = base + contador;
            contador++;
        }

        return username;
    }

    public List<Transportista> listarPorTipo(TipoTransporte tipo) {
        return transportistaRepository.findByTipoTransporteAndEstado(tipo, EstadoTransportista.ACTIVO);
    }

    public List<Transportista> listar() {
        return transportistaRepository.findAll();
    }

    @Transactional
    public Transportista actualizar(Long id, TransportistaRequestDTO dto) {
        Transportista existente = transportistaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transportista no encontrado"));

        // Guardar copia del estado anterior
        Transportista oldCopy = copiarEntidad(existente);

        // Actualizar campos
        existente.setNombre(dto.getNombre());
        existente.setApellidos(dto.getApellidos());
        existente.setDni(dto.getDni());
        existente.setEdad(dto.getEdad());
        existente.setTipoTransporte(dto.getTipoTransporte());
        existente.setPlaca(dto.getPlaca());
        existente.setVehiculoInfo(dto.getVehiculoInfo());
        existente.setCapacidad(dto.getCapacidad());

        if (dto.getEstado() != null && !dto.getEstado().isBlank()) {
            existente.setEstado(EstadoTransportista.valueOf(dto.getEstado().toUpperCase(Locale.ROOT)));
        }

        Transportista updated = transportistaRepository.save(existente);

        // Auditar actualización
        auditService.auditTransportista(
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
        Transportista existente = transportistaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transportista no encontrado"));

        // Guardar copia para auditoría
        Transportista oldCopy = copiarEntidad(existente);

        // Soft delete de BaseEntity (marca deletedAt y deletedBy)
        existente.softDelete(username);

        // 🔥 IMPORTANTE: Cambiar el estado explícitamente a INACTIVO
        existente.setEstado(EstadoTransportista.INACTIVO);

        transportistaRepository.save(existente);

        // Auditar eliminación
        auditService.auditTransportista(
                id,
                "DELETE",
                oldCopy,
                null,
                request
        );
    }

    public Transportista copiarEntidad(Transportista original) {
        Transportista copia = new Transportista();

        copia.setNombre(original.getNombre());
        copia.setApellidos(original.getApellidos());
        copia.setDni(original.getDni());
        copia.setEdad(original.getEdad());
        copia.setTipoTransporte(original.getTipoTransporte());
        copia.setPlaca(original.getPlaca());
        copia.setVehiculoInfo(original.getVehiculoInfo());
        copia.setCapacidad(original.getCapacidad());
        copia.setEstado(original.getEstado());

        return copia;
    }
}