package com.segurosbolivar.polizas.api.controller;

import com.segurosbolivar.polizas.api.dto.RiesgoResponse;
import com.segurosbolivar.polizas.api.mapper.PolizaMapper;
import com.segurosbolivar.polizas.application.service.RiesgoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Riesgos")
@RestController
@RequestMapping("/riesgos")
@RequiredArgsConstructor
public class RiesgoController {

    private final RiesgoService riesgoService;

    @Operation(summary = "Cancela un riesgo sin afectar los demás riesgos de la póliza")
    @PostMapping("/{id}/cancelar")
    public RiesgoResponse cancelar(@PathVariable Long id) {
        return PolizaMapper.toResponse(riesgoService.cancelar(id));
    }
}
