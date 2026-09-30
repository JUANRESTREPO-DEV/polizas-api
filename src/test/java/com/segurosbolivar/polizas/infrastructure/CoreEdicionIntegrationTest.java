package com.segurosbolivar.polizas.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Recorrido completo sobre HTTP real: la operación de negocio invoca el adaptador del CORE, que a su vez
 * llama al mock expuesto por la misma aplicación.
 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = {"classpath:db/limpiar.sql", "classpath:data.sql"})
class CoreEdicionIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void cancelarUnaPolizaDejaTrazaDelEnvioAlCore(CapturedOutput output) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", "123456");
        headers.set("X-Correlation-ID", "prueba-core-01");

        ResponseEntity<String> respuesta = restTemplate.postForEntity(
                "/polizas/5/cancelar", new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(output)
                .contains("[CORE-MOCK] Evento recibido para envío al CORE: evento=ACTUALIZACION polizaId=5")
                .contains("operacion=CANCELACION_POLIZA")
                .contains("CORE notificado: operacion=CANCELACION_POLIZA polizaId=5");
    }
}
