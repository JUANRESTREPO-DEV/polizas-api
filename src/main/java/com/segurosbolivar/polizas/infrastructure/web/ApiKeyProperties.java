package com.segurosbolivar.polizas.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * @param rutasPublicas patrones Ant excluidos de la validación (documentación, consola H2, health check).
 */
@Validated
@ConfigurationProperties(prefix = "seguridad.api-key")
public record ApiKeyProperties(@NotBlank String valor, List<String> rutasPublicas) {

    public ApiKeyProperties {
        rutasPublicas = rutasPublicas == null ? List.of() : List.copyOf(rutasPublicas);
    }
}
