package com.segurosbolivar.polizas.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RiesgoRequest(
        @NotBlank @Size(max = 200) String direccionInmueble,
        @NotBlank @Size(max = 80) String ciudad,
        @NotNull @Valid TerceroDto asegurado,
        @NotNull @Valid TerceroDto beneficiario,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal canonMensual) {
}
