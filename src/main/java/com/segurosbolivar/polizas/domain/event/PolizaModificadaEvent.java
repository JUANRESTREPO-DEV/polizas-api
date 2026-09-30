package com.segurosbolivar.polizas.domain.event;

/**
 * Cambio de estado confirmado sobre una póliza o uno de sus riesgos.
 * {@code riesgoId} es nulo cuando la operación aplica a la póliza completa.
 */
public record PolizaModificadaEvent(Long polizaId, Long riesgoId, OperacionPoliza operacion) {

    public static PolizaModificadaEvent dePoliza(Long polizaId, OperacionPoliza operacion) {
        return new PolizaModificadaEvent(polizaId, null, operacion);
    }

    public static PolizaModificadaEvent deRiesgo(Long polizaId, Long riesgoId, OperacionPoliza operacion) {
        return new PolizaModificadaEvent(polizaId, riesgoId, operacion);
    }
}
