package com.segurosbolivar.polizas.application.service;

import com.segurosbolivar.polizas.application.port.CoreEdicionPort;
import com.segurosbolivar.polizas.domain.event.PolizaModificadaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * El CORE solo se notifica cuando el cambio ya quedó confirmado localmente; así una transacción
 * revertida nunca genera una edición fantasma en el sistema legado.
 */
@Component
@RequiredArgsConstructor
class CoreSincronizacionListener {

    private final CoreEdicionPort coreEdicionPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void alConfirmarCambio(PolizaModificadaEvent evento) {
        coreEdicionPort.notificarActualizacion(evento);
    }
}
