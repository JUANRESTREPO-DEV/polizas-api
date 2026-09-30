package com.segurosbolivar.polizas.infrastructure.persistence;

import com.segurosbolivar.polizas.domain.model.HistorialRenovacion;
import com.segurosbolivar.polizas.domain.repository.HistorialRenovacionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaHistorialRenovacionRepository
        extends JpaRepository<HistorialRenovacion, Long>, HistorialRenovacionRepository {
}
