package com.segurosbolivar.polizas.domain.exception;

public class RiesgoNoEncontradoException extends RecursoNoEncontradoException {

    public RiesgoNoEncontradoException(Long riesgoId) {
        super("RIESGO_NO_ENCONTRADO", "No existe el riesgo con id " + riesgoId);
    }
}
