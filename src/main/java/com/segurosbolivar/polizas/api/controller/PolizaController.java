package com.segurosbolivar.polizas.api.controller;

import com.segurosbolivar.polizas.api.dto.PolizaResponse;
import com.segurosbolivar.polizas.api.dto.RiesgoRequest;
import com.segurosbolivar.polizas.api.dto.RiesgoResponse;
import com.segurosbolivar.polizas.api.mapper.PolizaMapper;
import com.segurosbolivar.polizas.application.service.PolizaService;
import com.segurosbolivar.polizas.domain.model.EstadoPoliza;
import com.segurosbolivar.polizas.domain.model.Riesgo;
import com.segurosbolivar.polizas.domain.model.TipoPoliza;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@Tag(name = "Pólizas")
@RestController
@RequestMapping("/polizas")
@RequiredArgsConstructor
public class PolizaController {

    private final PolizaService polizaService;

    @Operation(summary = "Lista pólizas, opcionalmente filtradas por tipo y estado")
    @GetMapping
    public List<PolizaResponse> listar(@RequestParam(required = false) TipoPoliza tipo,
                                       @RequestParam(required = false) EstadoPoliza estado) {
        return polizaService.listar(tipo, estado).stream().map(PolizaMapper::toResponse).toList();
    }

    @Operation(summary = "Lista los riesgos de una póliza, activos y cancelados")
    @GetMapping("/{id}/riesgos")
    public List<RiesgoResponse> listarRiesgos(@PathVariable Long id) {
        return polizaService.listarRiesgos(id).stream().map(PolizaMapper::toResponse).toList();
    }

    @Operation(summary = "Renueva la póliza ajustando canon y prima por IPC")
    @PostMapping("/{id}/renovar")
    public PolizaResponse renovar(@PathVariable Long id) {
        return PolizaMapper.toResponse(polizaService.renovar(id));
    }

    @Operation(summary = "Cancela la póliza y todos sus riesgos activos")
    @PostMapping("/{id}/cancelar")
    public PolizaResponse cancelar(@PathVariable Long id) {
        return PolizaMapper.toResponse(polizaService.cancelar(id));
    }

    @Operation(summary = "Agrega un riesgo a una póliza colectiva")
    @PostMapping("/{id}/riesgos")
    public ResponseEntity<RiesgoResponse> agregarRiesgo(@PathVariable Long id,
                                                        @Valid @RequestBody RiesgoRequest request) {
        Riesgo riesgo = polizaService.agregarRiesgo(id, PolizaMapper.toDomain(request));
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri())
                .body(PolizaMapper.toResponse(riesgo));
    }
}
