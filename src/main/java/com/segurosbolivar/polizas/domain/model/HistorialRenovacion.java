package com.segurosbolivar.polizas.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Traza inmutable de cada renovación. Permite reconstruir la evolución del canon y la prima,
 * requisito habitual de auditoría y de conciliación con el CORE.
 */
@Entity
@Table(name = "historial_renovacion")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HistorialRenovacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poliza_id", nullable = false)
    private Long polizaId;

    @Column(name = "fecha_renovacion", nullable = false)
    private LocalDateTime fechaRenovacion;

    @Column(name = "porcentaje_ipc", nullable = false, precision = 7, scale = 4)
    private BigDecimal porcentajeIpc;

    @Column(name = "canon_anterior", nullable = false, precision = 15, scale = 2)
    private BigDecimal canonAnterior;

    @Column(name = "canon_nuevo", nullable = false, precision = 15, scale = 2)
    private BigDecimal canonNuevo;

    @Column(name = "prima_anterior", nullable = false, precision = 17, scale = 2)
    private BigDecimal primaAnterior;

    @Column(name = "prima_nueva", nullable = false, precision = 17, scale = 2)
    private BigDecimal primaNueva;

    @Column(name = "inicio_vigencia_anterior", nullable = false)
    private LocalDate inicioVigenciaAnterior;

    @Column(name = "fin_vigencia_anterior", nullable = false)
    private LocalDate finVigenciaAnterior;

    @Column(name = "inicio_vigencia_nueva", nullable = false)
    private LocalDate inicioVigenciaNueva;

    @Column(name = "fin_vigencia_nueva", nullable = false)
    private LocalDate finVigenciaNueva;
}
