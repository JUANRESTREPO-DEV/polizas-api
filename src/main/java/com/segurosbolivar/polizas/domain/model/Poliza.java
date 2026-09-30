package com.segurosbolivar.polizas.domain.model;

import com.segurosbolivar.polizas.domain.exception.PolizaCanceladaException;
import com.segurosbolivar.polizas.domain.exception.RiesgoCanceladoException;
import com.segurosbolivar.polizas.domain.exception.RiesgoNoEncontradoException;
import com.segurosbolivar.polizas.domain.exception.TipoPolizaInvalidaException;
import com.segurosbolivar.polizas.domain.exception.UltimoRiesgoActivoException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Raíz del agregado. Toda regla que afecte estado, canon o prima se resuelve aquí para que
 * la consistencia entre la póliza y sus riesgos no dependa de la capa de servicio.
 *
 * <p>El canon de la póliza es la suma del canon de sus riesgos activos; en la individual coincide
 * con el de su único riesgo. La prima es siempre canon mensual x meses de vigencia.</p>
 */
@Entity
@Table(name = "poliza")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Poliza {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 20)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 12)
    private TipoPoliza tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private EstadoPoliza estado;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "tipoDocumento", column = @Column(name = "tomador_tipo_documento", nullable = false)),
            @AttributeOverride(name = "documento", column = @Column(name = "tomador_documento", nullable = false)),
            @AttributeOverride(name = "nombre", column = @Column(name = "tomador_nombre", nullable = false))
    })
    private Tercero tomador;

    @Column(name = "fecha_inicio_vigencia", nullable = false)
    private LocalDate fechaInicioVigencia;

    @Column(name = "fecha_fin_vigencia", nullable = false)
    private LocalDate fechaFinVigencia;

    @Column(name = "meses_vigencia", nullable = false)
    private int mesesVigencia;

    @Column(name = "canon_mensual", nullable = false, precision = 15, scale = 2)
    private BigDecimal canonMensual;

    @Column(name = "valor_prima", nullable = false, precision = 17, scale = 2)
    private BigDecimal valorPrima;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Version
    private Long version;

    @OneToMany(mappedBy = "poliza", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("id ASC")
    private List<Riesgo> riesgos = new ArrayList<>();

    private Poliza(String numero, TipoPoliza tipo, Tercero tomador, LocalDate inicioVigencia, int mesesVigencia) {
        if (mesesVigencia <= 0) {
            throw new IllegalArgumentException("La vigencia debe ser de al menos un mes");
        }
        this.numero = Objects.requireNonNull(numero);
        this.tipo = Objects.requireNonNull(tipo);
        this.tomador = Objects.requireNonNull(tomador);
        this.fechaInicioVigencia = Objects.requireNonNull(inicioVigencia);
        this.mesesVigencia = mesesVigencia;
        this.fechaFinVigencia = inicioVigencia.plusMonths(mesesVigencia);
        this.estado = EstadoPoliza.ACTIVA;
    }

    /**
     * En la póliza individual el arrendatario es a la vez tomador y asegurado.
     */
    public static Poliza individual(String numero, Riesgo riesgo, LocalDate inicioVigencia, int mesesVigencia) {
        Poliza poliza = new Poliza(numero, TipoPoliza.INDIVIDUAL, riesgo.getAsegurado(), inicioVigencia, mesesVigencia);
        poliza.vincular(riesgo);
        poliza.recalcularValores();
        return poliza;
    }

    public static Poliza colectiva(String numero, Tercero tomador, List<Riesgo> riesgos,
                                   LocalDate inicioVigencia, int mesesVigencia) {
        if (riesgos == null || riesgos.isEmpty()) {
            throw new IllegalArgumentException("Una póliza colectiva requiere al menos un riesgo");
        }
        Poliza poliza = new Poliza(numero, TipoPoliza.COLECTIVA, tomador, inicioVigencia, mesesVigencia);
        riesgos.forEach(poliza::vincular);
        poliza.recalcularValores();
        return poliza;
    }

    public List<Riesgo> getRiesgos() {
        return Collections.unmodifiableList(riesgos);
    }

    public boolean isCancelada() {
        return estado == EstadoPoliza.CANCELADA;
    }

    /**
     * Renueva por el mismo número de meses de la vigencia inicial, a continuación de la vigencia actual,
     * ajustando el canon de cada riesgo activo por el IPC.
     */
    public HistorialRenovacion renovar(BigDecimal porcentajeIpc, LocalDateTime momento) {
        validarNoCancelada("renovar");
        if (porcentajeIpc == null || porcentajeIpc.signum() < 0) {
            throw new IllegalArgumentException("El porcentaje de IPC no puede ser negativo");
        }

        BigDecimal canonAnterior = canonMensual;
        BigDecimal primaAnterior = valorPrima;
        LocalDate inicioAnterior = fechaInicioVigencia;
        LocalDate finAnterior = fechaFinVigencia;

        BigDecimal factor = BigDecimal.ONE.add(porcentajeIpc.divide(CIEN, 6, RoundingMode.HALF_UP));
        riesgos.stream().filter(Riesgo::isActivo).forEach(riesgo -> riesgo.ajustarCanon(factor));

        fechaInicioVigencia = finAnterior;
        fechaFinVigencia = finAnterior.plusMonths(mesesVigencia);
        estado = EstadoPoliza.RENOVADA;
        recalcularValores();

        return HistorialRenovacion.builder()
                .polizaId(id)
                .fechaRenovacion(momento)
                .porcentajeIpc(porcentajeIpc)
                .canonAnterior(canonAnterior)
                .canonNuevo(canonMensual)
                .primaAnterior(primaAnterior)
                .primaNueva(valorPrima)
                .inicioVigenciaAnterior(inicioAnterior)
                .finVigenciaAnterior(finAnterior)
                .inicioVigenciaNueva(fechaInicioVigencia)
                .finVigenciaNueva(fechaFinVigencia)
                .build();
    }

    /**
     * Cancela la póliza y, en cascada, todos sus riesgos activos. Canon y prima se conservan
     * como valores históricos; la liquidación de prima no devengada queda fuera de este alcance.
     */
    public void cancelar(LocalDateTime momento) {
        validarNoCancelada("cancelar");
        estado = EstadoPoliza.CANCELADA;
        fechaCancelacion = momento;
        riesgos.stream().filter(Riesgo::isActivo).forEach(riesgo -> riesgo.cancelar(momento));
    }

    public void agregarRiesgo(Riesgo riesgo) {
        if (tipo != TipoPoliza.COLECTIVA) {
            throw new TipoPolizaInvalidaException(id, tipo);
        }
        validarNoCancelada("agregar riesgos a");
        vincular(riesgo);
        recalcularValores();
    }

    /**
     * Cancela un riesgo sin afectar a los demás. No se permite dejar una póliza vigente sin riesgos
     * activos: en ese caso lo que procede es cancelar la póliza.
     */
    public Riesgo cancelarRiesgo(Long riesgoId, LocalDateTime momento) {
        Riesgo riesgo = riesgos.stream()
                .filter(r -> Objects.equals(r.getId(), riesgoId))
                .findFirst()
                .orElseThrow(() -> new RiesgoNoEncontradoException(riesgoId));

        validarNoCancelada("cancelar riesgos de");
        if (!riesgo.isActivo()) {
            throw new RiesgoCanceladoException(riesgoId);
        }
        if (riesgosActivos() == 1) {
            throw new UltimoRiesgoActivoException(riesgoId, id);
        }

        riesgo.cancelar(momento);
        recalcularValores();
        return riesgo;
    }

    private void vincular(Riesgo riesgo) {
        riesgo.asignarA(this);
        riesgos.add(riesgo);
    }

    private long riesgosActivos() {
        return riesgos.stream().filter(Riesgo::isActivo).count();
    }

    private void recalcularValores() {
        canonMensual = Montos.normalizar(riesgos.stream()
                .filter(Riesgo::isActivo)
                .map(Riesgo::getCanonMensual)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        valorPrima = Montos.normalizar(canonMensual.multiply(BigDecimal.valueOf(mesesVigencia)));
    }

    private void validarNoCancelada(String operacion) {
        if (isCancelada()) {
            throw new PolizaCanceladaException(id, operacion);
        }
    }
}
