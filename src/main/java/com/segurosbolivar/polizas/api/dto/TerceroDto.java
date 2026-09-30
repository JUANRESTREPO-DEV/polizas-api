package com.segurosbolivar.polizas.api.dto;

import com.segurosbolivar.polizas.domain.model.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TerceroDto(
        @NotNull TipoDocumento tipoDocumento,
        @NotBlank @Pattern(regexp = "[0-9A-Za-z-]{4,20}", message = "debe tener entre 4 y 20 caracteres alfanuméricos")
        String documento,
        @NotBlank @Size(max = 150) String nombre) {
}
