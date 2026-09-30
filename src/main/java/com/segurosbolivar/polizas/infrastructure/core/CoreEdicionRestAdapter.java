package com.segurosbolivar.polizas.infrastructure.core;

import com.segurosbolivar.polizas.application.port.CoreEdicionPort;
import com.segurosbolivar.polizas.domain.event.PolizaModificadaEvent;
import com.segurosbolivar.polizas.infrastructure.web.CorrelationIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente HTTP hacia el servicio de edición de WebLogic.
 *
 * <p>Un fallo del CORE no revierte la operación ya confirmada: se registra con el contexto necesario
 * para reprocesarla. En producción este adaptador publicaría en un outbox transaccional con reintentos
 * y DLQ (ver documento de arquitectura); aquí se mantiene síncrono por alcance de la prueba.</p>
 */
@Slf4j
@Component
class CoreEdicionRestAdapter implements CoreEdicionPort {

    private static final String API_KEY_HEADER = "api-key";

    private final RestClient restClient;
    private final CoreEdicionProperties properties;
    private final Environment environment;

    CoreEdicionRestAdapter(RestClient.Builder builder, CoreEdicionProperties properties, Environment environment) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        this.restClient = builder.requestFactory(requestFactory).build();
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void notificarActualizacion(PolizaModificadaEvent evento) {
        EventoCoreRequest payload = new EventoCoreRequest(
                EventoCoreRequest.ACTUALIZACION, evento.polizaId(), evento.riesgoId(), evento.operacion().name());
        try {
            restClient.post()
                    .uri(resolverUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (StringUtils.hasText(properties.apiKey())) {
                            headers.set(API_KEY_HEADER, properties.apiKey());
                        }
                        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
                        if (correlationId != null) {
                            headers.set(CorrelationIdFilter.HEADER, correlationId);
                        }
                    })
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("CORE notificado: operacion={} polizaId={} riesgoId={}",
                    evento.operacion(), evento.polizaId(), evento.riesgoId());
        } catch (RestClientException ex) {
            log.error("Fallo notificando al CORE, requiere reproceso: operacion={} polizaId={} riesgoId={} causa={}",
                    evento.operacion(), evento.polizaId(), evento.riesgoId(), ex.getMessage());
        }
    }

    /**
     * El puerto local solo se conoce cuando el servidor arrancó (p. ej. puerto aleatorio en pruebas),
     * por eso se resuelve en cada invocación y no al construir el bean.
     */
    private String resolverUrl() {
        String baseUrl = StringUtils.hasText(properties.baseUrl())
                ? properties.baseUrl()
                : "http://localhost:" + environment.getProperty("local.server.port",
                        environment.getProperty("server.port", "8080"));
        return baseUrl + properties.path();
    }
}
