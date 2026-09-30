package com.segurosbolivar.polizas.coremock;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * Simula el servicio de edición publicado en WebLogic. Solo deja evidencia en el log de que la
 * operación se intentó enviar al CORE; no tiene estado ni lógica de negocio.
 */
@Slf4j
@Tag(name = "CORE mock")
@RestController
@RequestMapping("/core-mock")
public class CoreMockController {

    @Operation(summary = "Recibe un evento de edición destinado al CORE y lo registra en el log")
    @PostMapping("/evento")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventoRecibido recibir(@Valid @RequestBody EventoCore evento) {
        log.info("[CORE-MOCK] Evento recibido para envío al CORE: evento={} polizaId={} riesgoId={} operacion={}",
                evento.evento(), evento.polizaId(), evento.riesgoId(), evento.operacion());
        return new EventoRecibido("RECIBIDO", evento.evento(), evento.polizaId(), OffsetDateTime.now());
    }

    public record EventoCore(@NotBlank String evento, @NotNull Long polizaId, Long riesgoId, String operacion) {
    }

    public record EventoRecibido(String estado, String evento, Long polizaId, OffsetDateTime fechaRecepcion) {
    }
}
