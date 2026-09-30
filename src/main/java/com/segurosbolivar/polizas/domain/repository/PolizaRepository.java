package com.segurosbolivar.polizas.domain.repository;

import com.segurosbolivar.polizas.domain.model.EstadoPoliza;
import com.segurosbolivar.polizas.domain.model.Poliza;
import com.segurosbolivar.polizas.domain.model.TipoPoliza;

import java.util.List;
import java.util.Optional;

public interface PolizaRepository {

    Optional<Poliza> findById(Long id);

    boolean existsById(Long id);

    List<Poliza> buscar(TipoPoliza tipo, EstadoPoliza estado);

    Poliza save(Poliza poliza);
}
