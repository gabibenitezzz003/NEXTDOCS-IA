package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.HallazgoValidacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HallazgoValidacionRepository extends JpaRepository<HallazgoValidacion, String> {

	@Query("SELECT h FROM HallazgoValidacion h WHERE h.ejecucion.id = :ejecucionId ORDER BY h.severidad DESC")
	List<HallazgoValidacion> listarPorEjecucion(@Param("ejecucionId") String ejecucionId);

	@Query("SELECT h FROM HallazgoValidacion h WHERE h.id = :id AND h.tenant.id = :tenantId")
	Optional<HallazgoValidacion> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query("SELECT COUNT(h) FROM HallazgoValidacion h WHERE h.ejecucion.documento.id = :documentoId "
			+ "AND h.sobreescrito = FALSE")
	long contarPorDocumento(@Param("documentoId") String documentoId);
}
