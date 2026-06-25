package com.example.backendastramaco.unitarias;

import com.example.backendastramaco.model.DocumentoPersonal;
import com.example.backendastramaco.model.enums.TipoDocumento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentoPersonalUnitTest {

    @Test
    @DisplayName("Debe lanzar excepción si SOAT o REVISION_TECNICA no tienen fechas")
    void validarReglasDeFechas_LanzaExcepcion_CuandoFaltanFechas() {
        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.SOAT);
        doc.setValor("SOAT-123");
        doc.setFechaEmision(null); // Provocamos el error intencionalmente

        // Invocamos el método privado usando Reflection
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(doc, "validarReglasDeFechas"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Para SOAT y REVISION_TECNICA es obligatorio registrar fecha de emision y vencimiento.");
    }

    @Test
    @DisplayName("Debe pasar validación si SOAT tiene sus fechas completas")
    void validarReglasDeFechas_Pasa_CuandoTieneFechas() {
        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.REVISION_TECNICA);
        doc.setFechaEmision(LocalDate.now());
        doc.setFechaVencimiento(LocalDate.now().plusYears(1));

        // No debe lanzar ninguna excepción
        ReflectionTestUtils.invokeMethod(doc, "validarReglasDeFechas");

        assertThat(doc.getFechaEmision()).isNotNull();
        assertThat(doc.getFechaVencimiento()).isNotNull();
    }

    @Test
    @DisplayName("Debe forzar fechas a null si es LICENCIA o TARJETA_CIRCULACION")
    void validarReglasDeFechas_FuerzaNull_CuandoEsLicencia() {
        DocumentoPersonal doc = new DocumentoPersonal();
        doc.setTipoDocumento(TipoDocumento.LICENCIA);
        // Le ponemos fechas intencionalmente
        doc.setFechaEmision(LocalDate.now());
        doc.setFechaVencimiento(LocalDate.now().plusDays(5));

        ReflectionTestUtils.invokeMethod(doc, "validarReglasDeFechas");

        // El método privado debió haberlas convertido en null
        assertThat(doc.getFechaEmision()).isNull();
        assertThat(doc.getFechaVencimiento()).isNull();
    }

    @Test
    @DisplayName("Probar constructor, getters, setters y builder generados por Lombok")
    void lombokCoverage() {
        // Probamos el Builder
        DocumentoPersonal doc = DocumentoPersonal.builder()
                .tipoDocumento(TipoDocumento.REVISION_TECNICA)
                .valor("REV-123")
                .activo(false)
                .build();

        // Probamos Setters
        doc.setId(10L);

        // Probamos Getters
        assertThat(doc.getId()).isEqualTo(10L);
        assertThat(doc.getTipoDocumento()).isEqualTo(TipoDocumento.REVISION_TECNICA);
        assertThat(doc.getValor()).isEqualTo("REV-123");
        assertThat(doc.getActivo()).isFalse();
    }
}