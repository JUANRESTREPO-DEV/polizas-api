package com.segurosbolivar.polizas.infrastructure.core;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * @param baseUrl URL de la capa media. Si se omite se apunta al mock embebido en esta misma instancia.
 */
@ConfigurationProperties(prefix = "core.edicion")
public record CoreEdicionProperties(
        String baseUrl,
        @DefaultValue("/core-mock/evento") String path,
        String apiKey,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout) {
}
