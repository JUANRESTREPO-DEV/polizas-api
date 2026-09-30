package com.segurosbolivar.polizas.domain;

import com.segurosbolivar.polizas.domain.exception.PolizaCanceladaException;
import com.segurosbolivar.polizas.domain.exception.RiesgoCanceladoException;
import com.segurosbolivar.polizas.domain.exception.TipoPolizaInvalidaException;
import com.segurosbolivar.polizas.domain.exception.UltimoRiesgoActivoException;
import com.segurosbolivar.polizas.domain.model.EstadoPoliza;
import com.segurosbolivar.polizas.domain.model.EstadoRiesgo;
import com.segurosbolivar.polizas.domain.model.HistorialRenovacion;
import com.segurosbolivar.polizas.domain.model.Poliza;
import com.segurosbolivar.polizas.domain.model.Riesgo;
import com.segurosbolivar.polizas.domain.model.Tercero;
import com.segurosbolivar.polizas.domain.model.TipoDocumento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolizaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 9, 29, 10, 0);
    private static final BigDecimal IPC = new BigDecimal("5.20");

    @Nested
    @DisplayName("Creación")
    class Creacion {

        @Test
        void individualTomaAlArrendatarioComoTomadorYCalculaPrima() {
            Poliza poliza = individual(new BigDecimal("2500000"), 12);

            assertThat(poliza.getTomador()).isEqualTo(poliza.getRiesgos().get(0).getAsegurado());
            assertThat(poliza.getCanonMensual()).isEqualByComparingTo("2500000");
            assertThat(poliza.getValorPrima()).isEqualByComparingTo("30000000");
            assertThat(poliza.getFechaFinVigencia()).isEqualTo(LocalDate.of(2027, 1, 1));
            assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.ACTIVA);
        }

        @Test
        void colectivaSumaElCanonDeSusRiesgos() {
            Poliza poliza = colectiva(6, "1800000", "2200000");

            assertThat(poliza.getCanonMensual()).isEqualByComparingTo("4000000");
            assertThat(poliza.getValorPrima()).isEqualByComparingTo("24000000");
        }

        @Test
        void colectivaSinRiesgosNoEsValida() {
            assertThatThrownBy(() -> Poliza.colectiva("COL-1", tercero("900123456"), List.of(),
                    LocalDate.of(2026, 1, 1), 12))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Renovación")
    class Renovacion {

        @Test
        void ajustaCanonYPrimaPorIpcYExtiendeLaVigenciaPorElMismoPeriodo() {
            Poliza poliza = individual(new BigDecimal("2500000"), 12);

            HistorialRenovacion historial = poliza.renovar(IPC, AHORA);

            assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
            assertThat(poliza.getCanonMensual()).isEqualByComparingTo("2630000.00");
            assertThat(poliza.getValorPrima()).isEqualByComparingTo("31560000.00");
            assertThat(poliza.getFechaInicioVigencia()).isEqualTo(LocalDate.of(2027, 1, 1));
            assertThat(poliza.getFechaFinVigencia()).isEqualTo(LocalDate.of(2028, 1, 1));
            assertThat(historial.getCanonAnterior()).isEqualByComparingTo("2500000");
            assertThat(historial.getPrimaNueva()).isEqualByComparingTo("31560000");
        }

        @Test
        void soloAjustaLosRiesgosActivosDeUnaColectiva() {
            Poliza poliza = colectiva(12, "1000000", "2000000");
            poliza.cancelarRiesgo(2L, AHORA);

            poliza.renovar(new BigDecimal("10"), AHORA);

            assertThat(poliza.getRiesgos().get(0).getCanonMensual()).isEqualByComparingTo("1100000");
            assertThat(poliza.getRiesgos().get(1).getCanonMensual()).isEqualByComparingTo("2000000");
            assertThat(poliza.getCanonMensual()).isEqualByComparingTo("1100000");
            assertThat(poliza.getValorPrima()).isEqualByComparingTo("13200000");
        }

        @Test
        void noPermiteRenovarUnaPolizaCancelada() {
            Poliza poliza = individual(new BigDecimal("2500000"), 12);
            poliza.cancelar(AHORA);

            assertThatThrownBy(() -> poliza.renovar(IPC, AHORA)).isInstanceOf(PolizaCanceladaException.class);
            assertThat(poliza.getCanonMensual()).isEqualByComparingTo("2500000");
        }
    }

    @Nested
    @DisplayName("Cancelación")
    class Cancelacion {

        @Test
        void cancelaEnCascadaTodosLosRiesgos() {
            Poliza poliza = colectiva(12, "1000000", "2000000", "1500000");

            poliza.cancelar(AHORA);

            assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.CANCELADA);
            assertThat(poliza.getFechaCancelacion()).isEqualTo(AHORA);
            assertThat(poliza.getRiesgos()).extracting(Riesgo::getEstado).containsOnly(EstadoRiesgo.CANCELADO);
        }

        @Test
        void noPermiteCancelarDosVeces() {
            Poliza poliza = individual(new BigDecimal("2500000"), 12);
            poliza.cancelar(AHORA);

            assertThatThrownBy(() -> poliza.cancelar(AHORA)).isInstanceOf(PolizaCanceladaException.class);
        }
    }

    @Nested
    @DisplayName("Riesgos")
    class Riesgos {

        @Test
        void agregarRiesgoAColectivaRecalculaCanonYPrima() {
            Poliza poliza = colectiva(12, "1000000");

            poliza.agregarRiesgo(riesgo("1500000"));

            assertThat(poliza.getRiesgos()).hasSize(2);
            assertThat(poliza.getCanonMensual()).isEqualByComparingTo("2500000");
            assertThat(poliza.getValorPrima()).isEqualByComparingTo("30000000");
        }

        @Test
        void unaIndividualNoAdmiteUnSegundoRiesgo() {
            Poliza poliza = individual(new BigDecimal("2500000"), 12);

            assertThatThrownBy(() -> poliza.agregarRiesgo(riesgo("1000000")))
                    .isInstanceOf(TipoPolizaInvalidaException.class);
            assertThat(poliza.getRiesgos()).hasSize(1);
        }

        @Test
        void noSeAgreganRiesgosAUnaColectivaCancelada() {
            Poliza poliza = colectiva(12, "1000000");
            poliza.cancelar(AHORA);

            assertThatThrownBy(() -> poliza.agregarRiesgo(riesgo("1000000")))
                    .isInstanceOf(PolizaCanceladaException.class);
        }

        @Test
        void cancelarUnRiesgoNoAfectaALosDemas() {
            Poliza poliza = colectiva(12, "1000000", "2000000");

            poliza.cancelarRiesgo(1L, AHORA);

            assertThat(poliza.getRiesgos().get(0).getEstado()).isEqualTo(EstadoRiesgo.CANCELADO);
            assertThat(poliza.getRiesgos().get(1).getEstado()).isEqualTo(EstadoRiesgo.ACTIVO);
            assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.ACTIVA);
            assertThat(poliza.getCanonMensual()).isEqualByComparingTo("2000000");
            assertThat(poliza.getValorPrima()).isEqualByComparingTo("24000000");
        }

        @Test
        void noPermiteCancelarUnRiesgoYaCancelado() {
            Poliza poliza = colectiva(12, "1000000", "2000000", "3000000");
            poliza.cancelarRiesgo(1L, AHORA);

            assertThatThrownBy(() -> poliza.cancelarRiesgo(1L, AHORA)).isInstanceOf(RiesgoCanceladoException.class);
        }

        @Test
        void noPermiteDejarLaPolizaSinRiesgosActivos() {
            Poliza poliza = colectiva(12, "1000000", "2000000");
            poliza.cancelarRiesgo(1L, AHORA);

            assertThatThrownBy(() -> poliza.cancelarRiesgo(2L, AHORA))
                    .isInstanceOf(UltimoRiesgoActivoException.class);
            assertThat(poliza.getRiesgos().get(1).isActivo()).isTrue();
        }
    }

    private static Poliza individual(BigDecimal canon, int meses) {
        Riesgo riesgo = Riesgo.nuevo("Carrera 7 # 72-41", "Bogotá D.C.", tercero("1020304050"),
                tercero("19345876"), canon);
        ReflectionTestUtils.setField(riesgo, "id", 1L);
        return Poliza.individual("IND-1", riesgo, LocalDate.of(2026, 1, 1), meses);
    }

    private static Poliza colectiva(int meses, String... canones) {
        List<Riesgo> riesgos = new java.util.ArrayList<>();
        for (int i = 0; i < canones.length; i++) {
            Riesgo riesgo = riesgo(canones[i]);
            ReflectionTestUtils.setField(riesgo, "id", (long) i + 1);
            riesgos.add(riesgo);
        }
        return Poliza.colectiva("COL-1", tercero("900123456"), riesgos, LocalDate.of(2026, 1, 1), meses);
    }

    private static Riesgo riesgo(String canon) {
        return Riesgo.nuevo("Calle 93 # 11-26", "Bogotá D.C.", tercero("1032456789"), tercero("51789456"),
                new BigDecimal(canon));
    }

    private static Tercero tercero(String documento) {
        return Tercero.of(TipoDocumento.CC, documento, "Tercero " + documento);
    }
}
