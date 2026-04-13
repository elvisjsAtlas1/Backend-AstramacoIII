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

@Service
@RequiredArgsConstructor
public class TransportistaService {

    private final TransportistaRepository transportistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public Transportista crear(TransportistaRequestDTO dto) {

        Transportista t = new Transportista();

        // 🔥 1. CREAR USUARIO AUTOMÁTICO
        Usuario usuario = new Usuario();

        String username = generarUsernameUnico(dto.nombre, dto.apellidos);

        usuario.setUsername(username);

        // 🔐 PASSWORD = DNI
        usuario.setPassword(passwordEncoder.encode(dto.dni));

        usuario.setRol(Rol.TRANSPORTISTA);
        usuario.setActivo(true);

        usuario = usuarioRepository.save(usuario);

        // 🔗 ASIGNAR USUARIO AL TRANSPORTISTA
        t.setUsuario(usuario);

        // 📋 DATOS
        t.setNombre(dto.nombre);
        t.setApellidos(dto.apellidos);
        t.setDni(dto.dni);
        t.setEdad(dto.edad);
        t.setTipoTransporte(dto.tipoTransporte);
        t.setPlaca(dto.placa);
        t.setVehiculoInfo(dto.vehiculoInfo);
        t.setCapacidad(dto.capacidad);

        // 🔥 ESTADO
        if (dto.estado == null || dto.estado.isEmpty()) {
            t.setEstado(EstadoTransportista.ACTIVO);
        } else {
            t.setEstado(EstadoTransportista.valueOf(dto.estado));
        }

        return transportistaRepository.save(t);
    }
    private String generarUsername(String nombre, String apellidos) {

        String primerNombre = nombre.split(" ")[0].toLowerCase();
        String primerApellido = apellidos.split(" ")[0].toLowerCase();

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
        return transportistaRepository
                .findByTipoTransporteAndEstado(tipo, EstadoTransportista.ACTIVO);
    }




    public List<Transportista> listar() {
        return transportistaRepository.findAll();
    }
}