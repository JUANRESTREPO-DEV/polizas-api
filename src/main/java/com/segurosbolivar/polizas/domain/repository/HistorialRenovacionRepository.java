package com.segurosbolivar.polizas.domain.repository;

import com.segurosbolivar.polizas.domain.model.HistorialRenovacion;

import java.util.List;

public interface HistorialRenovacionRepository {

    HistorialRenovacion save(HistorialRenovacion historial);

    List<HistorialRenovacion> findByPolizaIdOrderByFechaRenovacionAsc(Long polizaId);
}
