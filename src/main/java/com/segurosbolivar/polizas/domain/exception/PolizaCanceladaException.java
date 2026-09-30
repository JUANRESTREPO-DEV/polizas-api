package com.segurosbolivar.polizas.domain.exception;

public class PolizaCanceladaException extends ReglaNegocioException {

    public PolizaCanceladaException(Long polizaId, String operacion) {
        super("POLIZA_CANCELADA", "No es posible %s la póliza %d porque está cancelada".formatted(operacion, polizaId));
    }
}
