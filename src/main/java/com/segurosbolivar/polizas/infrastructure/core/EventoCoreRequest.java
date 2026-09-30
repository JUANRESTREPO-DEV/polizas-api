package com.segurosbolivar.polizas.infrastructure.core;

/**
 * Contrato del servicio de edición del CORE. {@code evento} identifica el tipo de mensaje;
 * {@code operacion} y {@code riesgoId} precisan qué cambió para que la capa media enrute la edición.
 */
public record EventoCoreRequest(String evento, Long polizaId, Long riesgoId, String operacion) {

    public static final String ACTUALIZACION = "ACTUALIZACION";
}
