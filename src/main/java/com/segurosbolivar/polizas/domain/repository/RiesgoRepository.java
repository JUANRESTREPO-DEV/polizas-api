package com.segurosbolivar.polizas.domain.repository;

import com.segurosbolivar.polizas.domain.model.Riesgo;

import java.util.List;
import java.util.Optional;

public interface RiesgoRepository {

    Optional<Riesgo> findById(Long id);

    List<Riesgo> findByPolizaIdOrderByIdAsc(Long polizaId);

    Riesgo saveAndFlush(Riesgo riesgo);
}
