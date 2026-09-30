package com.segurosbolivar.polizas.application.port;

import com.segurosbolivar.polizas.domain.event.PolizaModificadaEvent;

/**
 * Servicio agnóstico de edición expuesto por la capa media (WebLogic) que mantiene actualizado el CORE.
 */
public interface CoreEdicionPort {

    void notificarActualizacion(PolizaModificadaEvent evento);
}
