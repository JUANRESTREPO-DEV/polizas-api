package com.segurosbolivar.polizas.infrastructure.persistence;

import com.segurosbolivar.polizas.domain.model.Riesgo;
import com.segurosbolivar.polizas.domain.repository.RiesgoRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaRiesgoRepository extends JpaRepository<Riesgo, Long>, RiesgoRepository {
}
