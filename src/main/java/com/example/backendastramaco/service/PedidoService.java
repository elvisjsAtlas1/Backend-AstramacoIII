package com.example.backendastramaco.service;

import com.example.backendastramaco.dto.PedidoRequestDTO;
import com.example.backendastramaco.dto.PedidoResponseDTO;
import com.example.backendastramaco.model.Carga;
import com.example.backendastramaco.model.Pedido;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.enums.TipoMaterial;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.CargaRepository;
import com.example.backendastramaco.repository.PedidoRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final String TRANSPORTISTA_NO_ENCONTRADO = "Transportista no encontrado";
    private static final String TIPO_INCORRECTO = "El tipo de transporte del pedido no coincide con el transportista seleccionado";
    private static final String CARGA_NO_REGISTRADA = "El transportista no tiene carga registrada";
    private static final String MATERIAL_NO_DISPONIBLE = "El transportista no cuenta con ese material en su carga actual";
    private static final String STOCK_INSUFICIENTE = "Stock insuficiente para atender el pedido";
    private static final String MATERIAL_CAMIONERO_INVALIDO = "El transportista camionero solo puede trabajar con materiales PANDERETA o TECHO";
    private static final String CODIGO_VERIFICACION_POR_DEFECTO = "1234";

    private final PedidoRepository pedidoRepository;
    private final TransportistaRepository transportistaRepository;
    private final CargaRepository cargaRepository;

    @Transactional
    public PedidoResponseDTO crearPedido(PedidoRequestDTO dto) {
        Transportista transportista = transportistaRepository.findById(dto.getTransportistaId())
                .orElseThrow(() -> new RuntimeException(TRANSPORTISTA_NO_ENCONTRADO));

        validarTipoTransporte(dto, transportista);
        procesarCargaSiEsCamionero(dto, transportista);

        Pedido pedido = Pedido.builder()
                .clienteNombre(dto.getClienteNombre())
                .clienteTelefono(dto.getClienteTelefono())
                .direccionEnvio(dto.getDireccionEnvio())
                .tipoTransporte(dto.getTipoTransporte())
                .material(dto.getMaterial())
                .cantidad(dto.getCantidad())
                .montoTotal(dto.getMontoTotal())
                .adelanto(dto.getAdelanto())
                .piso(dto.getPiso())
                .horaEnvio(dto.getHoraEnvio())
                .transportista(transportista)
                .codigoVerificacion(CODIGO_VERIFICACION_POR_DEFECTO)
                .build();

        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        return toResponseDTO(pedidoGuardado);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listar() {
        return pedidoRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private void validarTipoTransporte(PedidoRequestDTO dto, Transportista transportista) {
        if (!transportista.getTipoTransporte().equals(dto.getTipoTransporte())) {
            throw new IllegalArgumentException(TIPO_INCORRECTO);
        }
    }

    private void procesarCargaSiEsCamionero(PedidoRequestDTO dto, Transportista transportista) {
        if (transportista.getTipoTransporte() != TipoTransporte.CAMIONERO) {
            return;
        }

        validarMaterialCamionero(dto.getMaterial());

        Carga carga = cargaRepository
                .findByTransportistaId(transportista.getId())
                .orElseThrow(() -> new RuntimeException(CARGA_NO_REGISTRADA));

        if (!carga.getTipoMaterial().equals(dto.getMaterial())) {
            throw new IllegalArgumentException(MATERIAL_NO_DISPONIBLE);
        }

        if (carga.getCantidadDisponible() < dto.getCantidad()) {
            throw new IllegalArgumentException(STOCK_INSUFICIENTE);
        }

        carga.setCantidadDisponible(carga.getCantidadDisponible() - dto.getCantidad());
        cargaRepository.save(carga);
    }

    private void validarMaterialCamionero(TipoMaterial material) {
        if (material != TipoMaterial.PANDERETA && material != TipoMaterial.TECHO) {
            throw new IllegalArgumentException(MATERIAL_CAMIONERO_INVALIDO);
        }
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPorTransportista(Long transportistaId) {
        return pedidoRepository.findByTransportistaIdOrderByHoraEnvioDesc(transportistaId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private PedidoResponseDTO toResponseDTO(Pedido pedido) {
        Transportista transportista = pedido.getTransportista();

        return PedidoResponseDTO.builder()
                .id(pedido.getId())
                .clienteNombre(pedido.getClienteNombre())
                .clienteTelefono(pedido.getClienteTelefono())
                .direccionEnvio(pedido.getDireccionEnvio())
                .tipoTransporte(pedido.getTipoTransporte())
                .material(pedido.getMaterial())
                .cantidad(pedido.getCantidad())
                .montoTotal(pedido.getMontoTotal())
                .adelanto(pedido.getAdelanto())
                .piso(pedido.getPiso())
                .horaEnvio(pedido.getHoraEnvio())
                .transportistaId(transportista != null ? transportista.getId() : null)
                .transportistaNombre(obtenerNombreCompletoTransportista(transportista))
                .estado(pedido.getEstado())
                .codigoVerificacion(pedido.getCodigoVerificacion())
                .build();
    }

    private String obtenerNombreCompletoTransportista(Transportista transportista) {
        if (transportista == null) {
            return null;
        }

        String nombre = transportista.getNombre() != null ? transportista.getNombre().trim() : "";
        String apellidos = transportista.getApellidos() != null ? transportista.getApellidos().trim() : "";

        return (nombre + " " + apellidos).trim();
    }
}