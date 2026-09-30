package com.segurosbolivar.polizas.infrastructure.persistence;

import com.segurosbolivar.polizas.domain.model.EstadoPoliza;
import com.segurosbolivar.polizas.domain.model.Poliza;
import com.segurosbolivar.polizas.domain.model.TipoPoliza;
import com.segurosbolivar.polizas.domain.repository.PolizaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface JpaPolizaRepository extends JpaRepository<Poliza, Long>, PolizaRepository {

    @Override
    @Query("""
            select p from Poliza p
            where (:tipo is null or p.tipo = :tipo)
              and (:estado is null or p.estado = :estado)
            order by p.id
            """)
    List<Poliza> buscar(@Param("tipo") TipoPoliza tipo, @Param("estado") EstadoPoliza estado);
}
