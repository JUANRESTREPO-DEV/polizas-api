package com.segurosbolivar.polizas.domain.exception;

public abstract class RecursoNoEncontradoException extends ReglaNegocioException {

    protected RecursoNoEncontradoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
