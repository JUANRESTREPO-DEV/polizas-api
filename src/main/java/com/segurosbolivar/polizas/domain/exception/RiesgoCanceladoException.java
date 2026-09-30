package com.segurosbolivar.polizas.domain.exception;

public class RiesgoCanceladoException extends ReglaNegocioException {

    public RiesgoCanceladoException(Long riesgoId) {
        super("RIESGO_CANCELADO", "El riesgo %d ya se encuentra cancelado".formatted(riesgoId));
    }
}
