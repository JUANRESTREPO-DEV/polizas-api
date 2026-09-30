package com.segurosbolivar.polizas.domain.exception;

import lombok.Getter;

/**
 * Violación de una regla de negocio. El {@code codigo} es estable y lo consumen los clientes
 * para decidir cómo reaccionar sin interpretar el mensaje.
 */
@Getter
public abstract class ReglaNegocioException extends RuntimeException {

    private final String codigo;

    protected ReglaNegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }
}
