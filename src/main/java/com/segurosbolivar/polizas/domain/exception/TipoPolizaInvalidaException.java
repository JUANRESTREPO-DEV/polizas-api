package com.segurosbolivar.polizas.domain.exception;

import com.segurosbolivar.polizas.domain.model.TipoPoliza;

public class TipoPolizaInvalidaException extends ReglaNegocioException {

    public TipoPolizaInvalidaException(Long polizaId, TipoPoliza tipoActual) {
        super("TIPO_POLIZA_INVALIDO",
                "La póliza %d es %s; solo las pólizas COLECTIVA admiten nuevos riesgos".formatted(polizaId, tipoActual));
    }
}
