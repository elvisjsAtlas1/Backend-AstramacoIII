package com.example.backendastramaco.dto;

import com.example.backendastramaco.model.enums.TipoDocumento;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DocumentoPersonalRequestDTO {

    private TipoDocumento tipoDocumento;
    private String valor;
    private LocalDate fechaVencimiento;
}