package com.segurosbolivar.polizas.api.error;

import com.segurosbolivar.polizas.domain.exception.RecursoNoEncontradoException;
import com.segurosbolivar.polizas.domain.exception.ReglaNegocioException;
import com.segurosbolivar.polizas.domain.exception.TipoPolizaInvalidaException;
import com.segurosbolivar.polizas.infrastructure.web.CorrelationIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Traduce excepciones a RFC 7807 (application/problem+json). Todas las respuestas llevan un
 * {@code codigo} estable y el {@code correlationId} para cruzar el error con los logs.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TIPO_BASE = "urn:polizas:error:";

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail recursoNoEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getCodigo(), ex.getMessage());
    }

    @ExceptionHandler(TipoPolizaInvalidaException.class)
    ProblemDetail tipoPolizaInvalida(TipoPolizaInvalidaException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Operación no permitida para el tipo de póliza",
                ex.getCodigo(), ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ProblemDetail reglaNegocio(ReglaNegocioException ex) {
        log.info("Regla de negocio incumplida [{}]: {}", ex.getCodigo(), ex.getMessage());
        return problema(HttpStatus.CONFLICT, "Conflicto con el estado actual", ex.getCodigo(), ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concurrencia(OptimisticLockingFailureException ex) {
        return problema(HttpStatus.CONFLICT, "Modificación concurrente", "MODIFICACION_CONCURRENTE",
                "El recurso fue modificado por otra operación; consulte su estado y reintente");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Parámetro inválido", "PARAMETRO_INVALIDO",
                "El valor '%s' no es válido para el parámetro '%s'".formatted(ex.getValue(), ex.getName()));
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail errorNoControlado(Exception ex) {
        log.error("Error no controlado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "ERROR_INTERNO",
                "Ocurrió un error inesperado; reporte el correlationId a soporte");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "campo", error.getField(),
                        "mensaje", String.valueOf(error.getDefaultMessage())))
                .toList();

        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Solicitud inválida", "SOLICITUD_INVALIDA",
                "La solicitud contiene %d campo(s) inválido(s)".formatted(errores.size()));
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, @NonNull HttpHeaders headers,
                                                          @NonNull HttpStatusCode statusCode,
                                                          @NonNull WebRequest request) {
        if (body instanceof ProblemDetail problema && problema.getProperties() == null) {
            enriquecer(problema, "ERROR_" + statusCode.value());
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String codigo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);
        problema.setType(URI.create(TIPO_BASE + codigo.toLowerCase(Locale.ROOT).replace('_', '-')));
        enriquecer(problema, codigo);
        return problema;
    }

    private void enriquecer(ProblemDetail problema, String codigo) {
        problema.setProperty("codigo", codigo);
        problema.setProperty("timestamp", OffsetDateTime.now().toString());
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            problema.setProperty("correlationId", correlationId);
        }
    }
}
