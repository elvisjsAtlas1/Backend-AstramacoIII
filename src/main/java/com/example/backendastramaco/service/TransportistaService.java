package com.example.backendastramaco.service;

import com.example.backendastramaco.dto.TransportistaRequestDTO;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.enums.EstadoTransportista;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TransportistaService {

    private final TransportistaRepository transportistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

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

        return transportistaRepository.save(t);
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
}