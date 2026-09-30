package com.segurosbolivar.polizas.api.mapper;

import com.segurosbolivar.polizas.api.dto.PolizaResponse;
import com.segurosbolivar.polizas.api.dto.RiesgoRequest;
import com.segurosbolivar.polizas.api.dto.RiesgoResponse;
import com.segurosbolivar.polizas.api.dto.TerceroDto;
import com.segurosbolivar.polizas.domain.model.Poliza;
import com.segurosbolivar.polizas.domain.model.Riesgo;
import com.segurosbolivar.polizas.domain.model.Tercero;

public final class PolizaMapper {

    private PolizaMapper() {
    }

    public static PolizaResponse toResponse(Poliza poliza) {
        return new PolizaResponse(
                poliza.getId(),
                poliza.getNumero(),
                poliza.getTipo(),
                poliza.getEstado(),
                toDto(poliza.getTomador()),
                poliza.getFechaInicioVigencia(),
                poliza.getFechaFinVigencia(),
                poliza.getMesesVigencia(),
                poliza.getCanonMensual(),
                poliza.getValorPrima(),
                poliza.getFechaCancelacion());
    }

    public static RiesgoResponse toResponse(Riesgo riesgo) {
        return new RiesgoResponse(
                riesgo.getId(),
                riesgo.getPoliza().getId(),
                riesgo.getEstado(),
                riesgo.getDireccionInmueble(),
                riesgo.getCiudad(),
                toDto(riesgo.getAsegurado()),
                toDto(riesgo.getBeneficiario()),
                riesgo.getCanonMensual(),
                riesgo.getFechaCancelacion());
    }

    public static Riesgo toDomain(RiesgoRequest request) {
        return Riesgo.nuevo(
                request.direccionInmueble().trim(),
                request.ciudad().trim(),
                toDomain(request.asegurado()),
                toDomain(request.beneficiario()),
                request.canonMensual());
    }

    private static TerceroDto toDto(Tercero tercero) {
        return new TerceroDto(tercero.getTipoDocumento(), tercero.getDocumento(), tercero.getNombre());
    }

    private static Tercero toDomain(TerceroDto dto) {
        return Tercero.of(dto.tipoDocumento(), dto.documento().trim(), dto.nombre().trim());
    }
}
