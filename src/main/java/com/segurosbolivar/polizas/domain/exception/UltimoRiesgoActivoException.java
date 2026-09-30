package com.segurosbolivar.polizas.domain.exception;

public class UltimoRiesgoActivoException extends ReglaNegocioException {

    public UltimoRiesgoActivoException(Long riesgoId, Long polizaId) {
        super("ULTIMO_RIESGO_ACTIVO",
                "El riesgo %d es el único activo de la póliza %d; para retirarlo se debe cancelar la póliza"
                        .formatted(riesgoId, polizaId));
    }
}
