package com.segurosbolivar.polizas.api.dto;

import com.segurosbolivar.polizas.domain.model.EstadoPoliza;
import com.segurosbolivar.polizas.domain.model.TipoPoliza;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PolizaResponse(
        Long id,
        String numero,
        TipoPoliza tipo,
        EstadoPoliza estado,
        TerceroDto tomador,
        LocalDate fechaInicioVigencia,
        LocalDate fechaFinVigencia,
        int mesesVigencia,
        BigDecimal canonMensual,
        BigDecimal valorPrima,
        LocalDateTime fechaCancelacion) {
}
