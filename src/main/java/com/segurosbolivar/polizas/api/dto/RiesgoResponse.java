package com.segurosbolivar.polizas.api.dto;

import com.segurosbolivar.polizas.domain.model.EstadoRiesgo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RiesgoResponse(
        Long id,
        Long polizaId,
        EstadoRiesgo estado,
        String direccionInmueble,
        String ciudad,
        TerceroDto asegurado,
        TerceroDto beneficiario,
        BigDecimal canonMensual,
        LocalDateTime fechaCancelacion) {
}
