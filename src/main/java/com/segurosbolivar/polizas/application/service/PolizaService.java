package com.segurosbolivar.polizas.application.service;

import com.segurosbolivar.polizas.domain.event.OperacionPoliza;
import com.segurosbolivar.polizas.domain.event.PolizaModificadaEvent;
import com.segurosbolivar.polizas.domain.exception.PolizaNoEncontradaException;
import com.segurosbolivar.polizas.domain.model.EstadoPoliza;
import com.segurosbolivar.polizas.domain.model.HistorialRenovacion;
import com.segurosbolivar.polizas.domain.model.Poliza;
import com.segurosbolivar.polizas.domain.model.Riesgo;
import com.segurosbolivar.polizas.domain.model.TipoPoliza;
import com.segurosbolivar.polizas.domain.repository.HistorialRenovacionRepository;
import com.segurosbolivar.polizas.domain.repository.PolizaRepository;
import com.segurosbolivar.polizas.domain.repository.RiesgoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolizaService {

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;
    private final HistorialRenovacionRepository historialRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RenovacionProperties renovacionProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Poliza> listar(TipoPoliza tipo, EstadoPoliza estado) {
        return polizaRepository.buscar(tipo, estado);
    }

    @Transactional(readOnly = true)
    public List<Riesgo> listarRiesgos(Long polizaId) {
        if (!polizaRepository.existsById(polizaId)) {
            throw new PolizaNoEncontradaException(polizaId);
        }
        return riesgoRepository.findByPolizaIdOrderByIdAsc(polizaId);
    }

    @Transactional
    public Poliza renovar(Long polizaId) {
        Poliza poliza = obtener(polizaId);
        HistorialRenovacion historial = poliza.renovar(renovacionProperties.ipcPorcentaje(), LocalDateTime.now(clock));
        historialRepository.save(historial);

        log.info("Póliza {} renovada: canon {} -> {}, prima {} -> {}, vigencia {} a {}",
                polizaId, historial.getCanonAnterior(), historial.getCanonNuevo(),
                historial.getPrimaAnterior(), historial.getPrimaNueva(),
                historial.getInicioVigenciaNueva(), historial.getFinVigenciaNueva());

        eventPublisher.publishEvent(PolizaModificadaEvent.dePoliza(polizaId, OperacionPoliza.RENOVACION_POLIZA));
        return poliza;
    }

    @Transactional
    public Poliza cancelar(Long polizaId) {
        Poliza poliza = obtener(polizaId);
        poliza.cancelar(LocalDateTime.now(clock));

        log.info("Póliza {} cancelada junto con sus riesgos activos", polizaId);
        eventPublisher.publishEvent(PolizaModificadaEvent.dePoliza(polizaId, OperacionPoliza.CANCELACION_POLIZA));
        return poliza;
    }

    @Transactional
    public Riesgo agregarRiesgo(Long polizaId, Riesgo riesgo) {
        Poliza poliza = obtener(polizaId);
        poliza.agregarRiesgo(riesgo);
        Riesgo guardado = riesgoRepository.saveAndFlush(riesgo);

        log.info("Riesgo {} agregado a la póliza {}", guardado.getId(), polizaId);
        eventPublisher.publishEvent(
                PolizaModificadaEvent.deRiesgo(polizaId, guardado.getId(), OperacionPoliza.ADICION_RIESGO));
        return guardado;
    }

    private Poliza obtener(Long polizaId) {
        return polizaRepository.findById(polizaId).orElseThrow(() -> new PolizaNoEncontradaException(polizaId));
    }
}
