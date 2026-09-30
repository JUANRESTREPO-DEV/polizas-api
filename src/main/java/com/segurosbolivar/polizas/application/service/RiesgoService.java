package com.segurosbolivar.polizas.application.service;

import com.segurosbolivar.polizas.domain.event.OperacionPoliza;
import com.segurosbolivar.polizas.domain.event.PolizaModificadaEvent;
import com.segurosbolivar.polizas.domain.exception.RiesgoNoEncontradoException;
import com.segurosbolivar.polizas.domain.model.Poliza;
import com.segurosbolivar.polizas.domain.model.Riesgo;
import com.segurosbolivar.polizas.domain.repository.RiesgoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiesgoService {

    private final RiesgoRepository riesgoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public Riesgo cancelar(Long riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new RiesgoNoEncontradoException(riesgoId));
        Poliza poliza = riesgo.getPoliza();
        poliza.cancelarRiesgo(riesgoId, LocalDateTime.now(clock));

        log.info("Riesgo {} de la póliza {} cancelado; nuevo canon {} y prima {}",
                riesgoId, poliza.getId(), poliza.getCanonMensual(), poliza.getValorPrima());
        eventPublisher.publishEvent(
                PolizaModificadaEvent.deRiesgo(poliza.getId(), riesgoId, OperacionPoliza.CANCELACION_RIESGO));
        return riesgo;
    }
}
