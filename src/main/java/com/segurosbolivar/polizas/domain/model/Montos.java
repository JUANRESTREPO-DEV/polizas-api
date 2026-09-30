package com.segurosbolivar.polizas.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Montos {

    static final int ESCALA = 2;

    private Montos() {
    }

    static BigDecimal normalizar(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }
}
