package com.example.backendastramaco.service;

import com.example.backendastramaco.dto.AumentarCargaRequestDTO;
import com.example.backendastramaco.dto.CargaRequestDTO;
import com.example.backendastramaco.dto.CargaResponseDTO;
import com.example.backendastramaco.model.Carga;
import com.example.backendastramaco.model.Transportista;
import com.example.backendastramaco.model.enums.TipoMaterial;
import com.example.backendastramaco.model.enums.TipoTransporte;
import com.example.backendastramaco.repository.CargaRepository;
import com.example.backendastramaco.repository.TransportistaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CargaService {

    private static final String TRANSPORTISTA_NO_EXISTE = "Transportista no existe";
    private static final String SOLO_CAMIONEROS = "Solo los transportistas CAMIONERO pueden manejar carga";
    private static final String MATERIAL_INVALIDO = "El transportista CAMIONERO solo puede registrar PANDERETA o TECHO";
    private static final String CARGA_NO_REGISTRADA = "El transportista no tiene carga registrada";
    private static final String MATERIAL_DISTINTO = "Solo se puede aumentar si el material es el mismo que la carga actual";

    private final CargaRepository cargaRepository;
    private final TransportistaRepository transportistaRepository;

    @Transactional
    public CargaResponseDTO subirCargaActual(Long transportistaId, CargaRequestDTO request) {
        Transportista transportista = obtenerCamioneroValido(transportistaId);
        validarMaterialCamionero(request.getTipoMaterial());

        Carga carga = cargaRepository.findByTransportistaId(transportistaId)
                .orElse(
                        Carga.builder()
                                .transportista(transportista)
                                .build()
                );

        carga.setTipoMaterial(request.getTipoMaterial());
        carga.setCantidadDisponible(request.getCantidadDisponible());

        return toResponseDTO(cargaRepository.save(carga));
    }

    @Transactional
    public CargaResponseDTO aumentarCargaActual(Long transportistaId, AumentarCargaRequestDTO request) {
        obtenerCamioneroValido(transportistaId);
        validarMaterialCamionero(request.getTipoMaterial());

        Carga carga = cargaRepository.findByTransportistaId(transportistaId)
                .orElseThrow(() -> new RuntimeException(CARGA_NO_REGISTRADA));

        if (!carga.getTipoMaterial().equals(request.getTipoMaterial())) {
            throw new IllegalArgumentException(MATERIAL_DISTINTO);
        }

        carga.setCantidadDisponible(carga.getCantidadDisponible() + request.getCantidadAgregar());

        return toResponseDTO(cargaRepository.save(carga));
    }

    @Transactional(readOnly = true)
    public CargaResponseDTO obtenerCarga(Long transportistaId) {
        obtenerCamioneroValido(transportistaId);

        return cargaRepository.findByTransportistaId(transportistaId)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("El transportista aún no tiene carga registrada"));
    }

    @Transactional(readOnly = true)
    public List<CargaResponseDTO> listarTodas() {
        return cargaRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private Transportista obtenerCamioneroValido(Long transportistaId) {
        Transportista transportista = transportistaRepository.findById(transportistaId)
                .orElseThrow(() -> new RuntimeException(TRANSPORTISTA_NO_EXISTE));

        if (transportista.getTipoTransporte() != TipoTransporte.CAMIONERO) {
            throw new IllegalArgumentException(SOLO_CAMIONEROS);
        }

        return transportista;
    }

    private void validarMaterialCamionero(TipoMaterial tipoMaterial) {
        if (tipoMaterial != TipoMaterial.PANDERETA && tipoMaterial != TipoMaterial.TECHO) {
            throw new IllegalArgumentException(MATERIAL_INVALIDO);
        }
    }



    private CargaResponseDTO toResponseDTO(Carga carga) {
        Transportista transportista = carga.getTransportista();
        return CargaResponseDTO.builder()
                .id(carga.getId())
                .transportistaId(transportista.getId())
                .transportistaNombre((transportista.getNombre() + " " + transportista.getApellidos()).trim())
                .tipoMaterial(carga.getTipoMaterial())
                .cantidadDisponible(carga.getCantidadDisponible())
                .build();
    }
}