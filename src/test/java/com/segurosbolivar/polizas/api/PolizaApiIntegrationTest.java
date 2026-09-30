package com.segurosbolivar.polizas.api;

import com.segurosbolivar.polizas.application.port.CoreEdicionPort;
import com.segurosbolivar.polizas.domain.event.OperacionPoliza;
import com.segurosbolivar.polizas.domain.event.PolizaModificadaEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql(scripts = {"classpath:db/limpiar.sql", "classpath:data.sql"})
class PolizaApiIntegrationTest {

    private static final String API_KEY = "123456";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CoreEdicionPort coreEdicionPort;

    @Nested
    class Seguridad {

        @Test
        void rechazaPeticionesSinApiKey() throws Exception {
            mockMvc.perform(get("/polizas"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.codigo").value("API_KEY_INVALIDA"));
        }

        @Test
        void rechazaApiKeyIncorrecta() throws Exception {
            mockMvc.perform(get("/polizas").header("api-key", "654321"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void propagaElCorrelationIdRecibido() throws Exception {
            mockMvc.perform(conApiKey(get("/polizas")).header("X-Correlation-ID", "abc-123"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Correlation-ID", "abc-123"));
        }
    }

    @Nested
    class Consultas {

        @Test
        void listaTodasLasPolizasSinFiltros() throws Exception {
            mockMvc.perform(conApiKey(get("/polizas")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(6)));
        }

        @Test
        void filtraPorTipoYEstado() throws Exception {
            mockMvc.perform(conApiKey(get("/polizas").param("tipo", "COLECTIVA").param("estado", "ACTIVA")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].tipo", everyItem(is("COLECTIVA"))))
                    .andExpect(jsonPath("$[*].estado", everyItem(is("ACTIVA"))));
        }

        @Test
        void rechazaUnValorDeFiltroDesconocido() throws Exception {
            mockMvc.perform(conApiKey(get("/polizas").param("estado", "VENCIDA")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
        }

        @Test
        void listaLosRiesgosDeUnaPoliza() throws Exception {
            mockMvc.perform(conApiKey(get("/polizas/4/riesgos")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].polizaId").value(4));
        }

        @Test
        void respondeNotFoundParaPolizaInexistente() throws Exception {
            mockMvc.perform(conApiKey(get("/polizas/999/riesgos")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("POLIZA_NO_ENCONTRADA"))
                    .andExpect(jsonPath("$.correlationId", notNullValue()));
        }
    }

    @Nested
    class Renovacion {

        @Test
        void renuevaAjustandoCanonYPrimaYNotificaAlCore() throws Exception {
            mockMvc.perform(conApiKey(post("/polizas/1/renovar")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("RENOVADA"))
                    .andExpect(jsonPath("$.canonMensual").value(2630000.00))
                    .andExpect(jsonPath("$.valorPrima").value(31560000.00))
                    .andExpect(jsonPath("$.fechaInicioVigencia").value("2027-02-01"))
                    .andExpect(jsonPath("$.fechaFinVigencia").value("2028-02-01"));

            verify(coreEdicionPort).notificarActualizacion(
                    PolizaModificadaEvent.dePoliza(1L, OperacionPoliza.RENOVACION_POLIZA));
        }

        @Test
        void noRenuevaUnaPolizaCanceladaNiNotificaAlCore() throws Exception {
            mockMvc.perform(conApiKey(post("/polizas/2/renovar")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("POLIZA_CANCELADA"));

            verify(coreEdicionPort, never()).notificarActualizacion(any());
        }
    }

    @Nested
    class Cancelacion {

        @Test
        void cancelaLaPolizaYTodosSusRiesgos() throws Exception {
            mockMvc.perform(conApiKey(post("/polizas/4/cancelar")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CANCELADA"))
                    .andExpect(jsonPath("$.fechaCancelacion", notNullValue()));

            mockMvc.perform(conApiKey(get("/polizas/4/riesgos")))
                    .andExpect(jsonPath("$[*].estado", everyItem(is("CANCELADO"))));

            verify(coreEdicionPort).notificarActualizacion(
                    PolizaModificadaEvent.dePoliza(4L, OperacionPoliza.CANCELACION_POLIZA));
        }

        @Test
        void noCancelaDosVecesLaMismaPoliza() throws Exception {
            mockMvc.perform(conApiKey(post("/polizas/6/cancelar")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("POLIZA_CANCELADA"));
        }
    }

    @Nested
    class Riesgos {

        private static final String NUEVO_RIESGO = """
                {
                  "direccionInmueble": "Carrera 11 # 86-32 Apto 601",
                  "ciudad": "Bogotá D.C.",
                  "asegurado":    { "tipoDocumento": "CC", "documento": "1019876543", "nombre": "Natalia Suárez Pinzón" },
                  "beneficiario": { "tipoDocumento": "CC", "documento": "19876543",   "nombre": "Óscar Iván Beltrán Rey" },
                  "canonMensual": 2400000
                }
                """;

        @Test
        void agregaRiesgoAColectivaYRecalculaLaPrima() throws Exception {
            mockMvc.perform(conApiKey(post("/polizas/4/riesgos"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(NUEVO_RIESGO))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.estado").value("ACTIVO"))
                    .andExpect(jsonPath("$.polizaId").value(4));

            mockMvc.perform(conApiKey(get("/polizas").param("tipo", "COLECTIVA").param("estado", "ACTIVA")))
                    .andExpect(jsonPath("$[?(@.id == 4)].canonMensual").value(9500000.00))
                    .andExpect(jsonPath("$[?(@.id == 4)].valorPrima").value(114000000.00));

            verify(coreEdicionPort).notificarActualizacion(any(PolizaModificadaEvent.class));
        }

        @Test
        void rechazaAgregarRiesgoAPolizaIndividual() throws Exception {
            mockMvc.perform(conApiKey(post("/polizas/1/riesgos"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(NUEVO_RIESGO))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("TIPO_POLIZA_INVALIDO"));

            verify(coreEdicionPort, never()).notificarActualizacion(any());
        }

        @Test
        void validaElCuerpoDelRiesgo() throws Exception {
            mockMvc.perform(conApiKey(post("/polizas/4/riesgos"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "direccionInmueble": "", "ciudad": "Bogotá D.C.", "canonMensual": -1 }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                    .andExpect(jsonPath("$.errores", hasSize(4)));
        }

        @Test
        void cancelaUnRiesgoSinAfectarLosDemas() throws Exception {
            mockMvc.perform(conApiKey(post("/riesgos/5/cancelar")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CANCELADO"));

            mockMvc.perform(conApiKey(get("/polizas/4/riesgos")))
                    .andExpect(jsonPath("$[?(@.estado == 'ACTIVO')]", hasSize(2)));

            verify(coreEdicionPort).notificarActualizacion(
                    PolizaModificadaEvent.deRiesgo(4L, 5L, OperacionPoliza.CANCELACION_RIESGO));
        }

        @Test
        void noCancelaElUnicoRiesgoActivo() throws Exception {
            mockMvc.perform(conApiKey(post("/riesgos/1/cancelar")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("ULTIMO_RIESGO_ACTIVO"));
        }

        @Test
        void noCancelaRiesgosDeUnaPolizaCancelada() throws Exception {
            mockMvc.perform(conApiKey(post("/riesgos/9/cancelar")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("POLIZA_CANCELADA"));
        }
    }

    @Nested
    class CoreMock {

        @Test
        void aceptaElEventoDeActualizacion() throws Exception {
            mockMvc.perform(conApiKey(post("/core-mock/evento"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "evento": "ACTUALIZACION", "polizaId": 555 }
                                    """))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.estado").value("RECIBIDO"))
                    .andExpect(jsonPath("$.polizaId").value(555));
        }
    }

    private static MockHttpServletRequestBuilder conApiKey(MockHttpServletRequestBuilder request) {
        return request.header("api-key", API_KEY);
    }
}
