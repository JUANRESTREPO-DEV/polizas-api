package com.segurosbolivar.polizas.domain.exception;

public class PolizaNoEncontradaException extends RecursoNoEncontradoException {

    public PolizaNoEncontradaException(Long polizaId) {
        super("POLIZA_NO_ENCONTRADA", "No existe la póliza con id " + polizaId);
    }
}
