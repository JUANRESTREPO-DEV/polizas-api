package com.segurosbolivar.polizas.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AplicacionConfig {

    private static final String ESQUEMA_API_KEY = "apiKey";

    /**
     * Reloj explícito en la zona de negocio: las fechas de vigencia y cancelación no deben depender
     * de la zona horaria del contenedor.
     */
    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("America/Bogota"));
    }

    @Bean
    OpenAPI polizasOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Gestión de Pólizas de Arrendamiento")
                        .version("v1")
                        .description("Consulta, renovación y cancelación de pólizas individuales y colectivas, "
                                + "y administración de riesgos de pólizas colectivas."))
                .components(new Components().addSecuritySchemes(ESQUEMA_API_KEY, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("api-key")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_API_KEY));
    }
}
