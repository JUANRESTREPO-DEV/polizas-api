package com.segurosbolivar.polizas.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Inmueble arrendado cubierto por la póliza. Su ciclo de vida lo gobierna el agregado {@link Poliza}:
 * las transiciones de estado se hacen siempre a través de la póliza para no romper sus invariantes.
 */
@Entity
@Table(name = "riesgo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Riesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poliza_id", nullable = false)
    private Poliza poliza;

    @Column(name = "direccion_inmueble", nullable = false, length = 200)
    private String direccionInmueble;

    @Column(name = "ciudad", nullable = false, length = 80)
    private String ciudad;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "tipoDocumento", column = @Column(name = "asegurado_tipo_documento", nullable = false)),
            @AttributeOverride(name = "documento", column = @Column(name = "asegurado_documento", nullable = false)),
            @AttributeOverride(name = "nombre", column = @Column(name = "asegurado_nombre", nullable = false))
    })
    private Tercero asegurado;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "tipoDocumento", column = @Column(name = "beneficiario_tipo_documento", nullable = false)),
            @AttributeOverride(name = "documento", column = @Column(name = "beneficiario_documento", nullable = false)),
            @AttributeOverride(name = "nombre", column = @Column(name = "beneficiario_nombre", nullable = false))
    })
    private Tercero beneficiario;

    @Column(name = "canon_mensual", nullable = false, precision = 15, scale = 2)
    private BigDecimal canonMensual;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoRiesgo estado;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Version
    private Long version;

    private Riesgo(String direccionInmueble, String ciudad, Tercero asegurado, Tercero beneficiario,
                   BigDecimal canonMensual) {
        this.direccionInmueble = Objects.requireNonNull(direccionInmueble);
        this.ciudad = Objects.requireNonNull(ciudad);
        this.asegurado = Objects.requireNonNull(asegurado);
        this.beneficiario = Objects.requireNonNull(beneficiario);
        this.canonMensual = Montos.normalizar(canonMensual);
        this.estado = EstadoRiesgo.ACTIVO;
    }

    public static Riesgo nuevo(String direccionInmueble, String ciudad, Tercero asegurado, Tercero beneficiario,
                               BigDecimal canonMensual) {
        if (canonMensual == null || canonMensual.signum() <= 0) {
            throw new IllegalArgumentException("El canon mensual del riesgo debe ser mayor a cero");
        }
        return new Riesgo(direccionInmueble, ciudad, asegurado, beneficiario, canonMensual);
    }

    public boolean isActivo() {
        return estado == EstadoRiesgo.ACTIVO;
    }

    void asignarA(Poliza poliza) {
        this.poliza = poliza;
    }

    void cancelar(LocalDateTime momento) {
        this.estado = EstadoRiesgo.CANCELADO;
        this.fechaCancelacion = momento;
    }

    void ajustarCanon(BigDecimal factor) {
        this.canonMensual = Montos.normalizar(canonMensual.multiply(factor));
    }
}
