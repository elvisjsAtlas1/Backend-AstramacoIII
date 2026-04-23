package com.example.backendastramaco;

import com.example.backendastramaco.dto.TransportistaRequestDTO;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.Usuario;
import com.example.backendastramaco.model.enums.EstadoTransportista;
import com.example.backendastramaco.model.enums.Rol;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.TransportistaRepository;
import com.example.backendastramaco.repository.UsuarioRepository;
import com.example.backendastramaco.service.TransportistaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransportistaServiceTests {

    @Mock
    private TransportistaRepository transportistaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TransportistaService transportistaService;

    @Test
    @DisplayName("Debe crear transportista con usuario automático y estado ACTIVO por defecto")
    void crear_DebeCrearTransportistaConEstadoPorDefecto() {
        TransportistaRequestDTO dto = new TransportistaRequestDTO();
        dto.setNombre("Juan");
        dto.setApellidos("Perez");
        dto.setDni("12345678");
        dto.setEdad(30);
        dto.setTipoTransporte(TipoTransporte.CAMIONERO);
        dto.setPlaca("ABC-123");
        dto.setVehiculoInfo("Camion rojo");
        dto.setCapacidad(10.5);
        dto.setEstado(null);

        when(usuarioRepository.findByUsername("juan.perez")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("12345678")).thenReturn("clave-codificada");

        Usuario usuarioGuardado = Usuario.builder()
                .username("juan.perez")
                .password("clave-codificada")
                .rol(Rol.TRANSPORTISTA)
                .activo(true)
                .build();

        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(transportistaRepository.save(any(Transportista.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transportista resultado = transportistaService.crear(dto);

        assertNotNull(resultado);
        assertEquals(EstadoTransportista.ACTIVO, resultado.getEstado());
        assertNotNull(resultado.getUsuario());
        assertEquals("juan.perez", resultado.getUsuario().getUsername());
        assertEquals(Rol.TRANSPORTISTA, resultado.getUsuario().getRol());
        assertTrue(Boolean.TRUE.equals(resultado.getUsuario().getActivo()));

        verify(passwordEncoder).encode("12345678");
        verify(usuarioRepository).save(any(Usuario.class));
        verify(transportistaRepository).save(any(Transportista.class));
    }

    @Test
    @DisplayName("Debe generar username único cuando el username base ya existe")
    void crear_DebeGenerarUsernameUnico() {
        TransportistaRequestDTO dto = new TransportistaRequestDTO();
        dto.setNombre("Juan");
        dto.setApellidos("Perez");
        dto.setDni("12345678");
        dto.setEdad(30);
        dto.setTipoTransporte(TipoTransporte.CAMIONERO);
        dto.setPlaca("ABC-123");
        dto.setVehiculoInfo("Camion");
        dto.setCapacidad(8.0);
        dto.setEstado("ACTIVO");

        when(usuarioRepository.findByUsername("juan.perez"))
                .thenReturn(Optional.of(mock(Usuario.class)));
        when(usuarioRepository.findByUsername("juan.perez1"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("12345678")).thenReturn("clave-codificada");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transportistaRepository.save(any(Transportista.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transportista resultado = transportistaService.crear(dto);

        assertNotNull(resultado);
        assertNotNull(resultado.getUsuario());
        assertEquals("juan.perez1", resultado.getUsuario().getUsername());
    }

    @Test
    @DisplayName("Debe asignar estado enviado en el DTO")
    void crear_DebeAsignarEstadoDelDto() {
        TransportistaRequestDTO dto = new TransportistaRequestDTO();
        dto.setNombre("Ana");
        dto.setApellidos("Lopez");
        dto.setDni("87654321");
        dto.setEdad(28);
        dto.setTipoTransporte(TipoTransporte.VOLQUETERO);
        dto.setPlaca("XYZ-999");
        dto.setVehiculoInfo("Unidad azul");
        dto.setCapacidad(15.0);
        dto.setEstado("INACTIVO");

        when(usuarioRepository.findByUsername("ana.lopez")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("87654321")).thenReturn("clave-codificada");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transportistaRepository.save(any(Transportista.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transportista resultado = transportistaService.crear(dto);

        assertNotNull(resultado);
        assertEquals(EstadoTransportista.INACTIVO, resultado.getEstado());
    }

    @Test
    @DisplayName("Debe listar transportistas activos por tipo")
    void listarPorTipo_DebeRetornarTransportistasActivos() {
        when(transportistaRepository.findByTipoTransporteAndEstado(
                TipoTransporte.CAMIONERO,
                EstadoTransportista.ACTIVO
        )).thenReturn(java.util.List.of(new Transportista(), new Transportista()));

        var resultado = transportistaService.listarPorTipo(TipoTransporte.CAMIONERO);

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
    }

    @Test
    @DisplayName("Debe listar todos los transportistas")
    void listar_DebeRetornarTodos() {
        when(transportistaRepository.findAll())
                .thenReturn(java.util.List.of(new Transportista(), new Transportista(), new Transportista()));

        var resultado = transportistaService.listar();

        assertNotNull(resultado);
        assertEquals(3, resultado.size());
    }
}