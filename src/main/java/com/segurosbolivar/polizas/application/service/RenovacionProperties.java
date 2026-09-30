package com.segurosbolivar.polizas.application.service;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * @param ipcPorcentaje variación anual del IPC en puntos porcentuales (5.20 = 5,20 %).
 */
@Validated
@ConfigurationProperties(prefix = "polizas.renovacion")
public record RenovacionProperties(
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal ipcPorcentaje) {
}
