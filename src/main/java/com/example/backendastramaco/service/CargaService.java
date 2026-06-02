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
import com.example.backendastramaco.service.audit.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
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
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Transactional
    public CargaResponseDTO subirCargaActual(Long transportistaId, CargaRequestDTO requestDTO) {
        Transportista transportista = obtenerCamioneroValido(transportistaId);
        validarMaterialCamionero(requestDTO.getTipoMaterial());

        Carga carga = cargaRepository.findByTransportistaId(transportistaId)
                .orElse(
                        Carga.builder()
                                .transportista(transportista)
                                .build()
                );

        Carga oldCopy = copiarEntidad(carga);

        carga.setTipoMaterial(requestDTO.getTipoMaterial());
        carga.setCantidadDisponible(requestDTO.getCantidadDisponible());

        Carga saved = cargaRepository.save(carga);

        // Auditar creación o actualización
        String accion = oldCopy.getId() == null ? "CREATE" : "UPDATE";
        auditService.auditCarga(
                saved.getId(),
                transportistaId,
                accion,
                oldCopy.getId() == null ? null : oldCopy,
                saved,
                request
        );

        return toResponseDTO(saved);
    }

    @Transactional
    public CargaResponseDTO aumentarCargaActual(Long transportistaId, AumentarCargaRequestDTO requestDTO) {
        obtenerCamioneroValido(transportistaId);
        validarMaterialCamionero(requestDTO.getTipoMaterial());

        Carga carga = cargaRepository.findByTransportistaId(transportistaId)
                .orElseThrow(() -> new RuntimeException(CARGA_NO_REGISTRADA));

        if (!carga.getTipoMaterial().equals(requestDTO.getTipoMaterial())) {
            throw new IllegalArgumentException(MATERIAL_DISTINTO);
        }

        Carga oldCopy = copiarEntidad(carga);

        carga.setCantidadDisponible(carga.getCantidadDisponible() + requestDTO.getCantidadAgregar());

        Carga saved = cargaRepository.save(carga);

        // Auditar aumento de carga
        auditService.auditCarga(
                saved.getId(),
                transportistaId,
                "UPDATE",
                oldCopy,
                saved,
                request
        );

        return toResponseDTO(saved);
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

    @Transactional
    public void eliminarCarga(Long id, String username) {
        Carga existente = cargaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carga no encontrada"));

        Carga oldCopy = copiarEntidad(existente);

        // Auditar eliminación
        auditService.auditCarga(
                id,
                existente.getTransportista().getId(),
                "DELETE",
                oldCopy,
                null,
                request
        );

        // Soft delete o eliminar físicamente
        cargaRepository.delete(existente);
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

    private Carga copiarEntidad(Carga original) {
        Carga copia = new Carga();
        copia.setId(original.getId());
        copia.setTransportista(original.getTransportista());
        copia.setTipoMaterial(original.getTipoMaterial());
        copia.setCantidadDisponible(original.getCantidadDisponible());
        return copia;
    }
}