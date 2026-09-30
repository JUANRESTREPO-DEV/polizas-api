package com.segurosbolivar.polizas.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;

/**
 * Exige el header {@code api-key} en toda la API. Se implementa como filtro de servlet y no con
 * Spring Security porque el requisito es una llave estática única; con OAuth2/JWT (escenario real
 * detrás del API Gateway) se reemplazaría por un resource server.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "api-key";

    private final byte[] apiKeyEsperada;
    private final ApiKeyProperties properties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public ApiKeyFilter(ApiKeyProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.apiKeyEsperada = properties.valor().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return properties.rutasPublicas().stream().anyMatch(patron -> pathMatcher.match(patron, ruta));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibida = request.getHeader(HEADER);
        if (recibida != null && MessageDigest.isEqual(apiKeyEsperada, recibida.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }

        log.warn("Acceso rechazado a {} {}: api-key {}", request.getMethod(), request.getRequestURI(),
                recibida == null ? "ausente" : "inválida");
        escribirNoAutorizado(request, response, recibida == null);
    }

    private void escribirNoAutorizado(HttpServletRequest request, HttpServletResponse response, boolean ausente)
            throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                ausente ? "El header 'api-key' es obligatorio" : "El valor del header 'api-key' no es válido");
        problema.setType(URI.create("urn:polizas:error:api-key-invalida"));
        problema.setTitle("No autorizado");
        problema.setInstance(URI.create(request.getRequestURI()));
        problema.setProperty("codigo", "API_KEY_INVALIDA");
        problema.setProperty("timestamp", OffsetDateTime.now().toString());
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            problema.setProperty("correlationId", correlationId);
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problema);
    }
}
