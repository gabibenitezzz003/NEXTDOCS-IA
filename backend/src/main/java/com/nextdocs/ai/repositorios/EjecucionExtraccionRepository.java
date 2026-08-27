package com.nextdocs.ai.repositorios;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.EjecucionExtraccion;
import com.nextdocs.ai.enumeraciones.EstadoEjecucion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EjecucionExtraccionRepository extends JpaRepository<EjecucionExtraccion, String> {

	@Query("SELECT e FROM EjecucionExtraccion e WHERE e.documento.id = :documentoId ORDER BY e.alta DESC")
	List<EjecucionExtraccion> listarPorDocumento(@Param("documentoId") String documentoId);

	@Query("SELECT e FROM EjecucionExtraccion e WHERE e.documento.id = :documentoId AND e.estado = :estado "
			+ "ORDER BY e.alta DESC")
	List<EjecucionExtraccion> listarPorDocumentoYEstado(@Param("documentoId") String documentoId,
			@Param("estado") EstadoEjecucion estado);

	@Query("SELECT e FROM EjecucionExtraccion e WHERE e.id = :id AND e.tenant.id = :tenantId")
	Optional<EjecucionExtraccion> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query("SELECT COUNT(e) FROM EjecucionExtraccion e WHERE e.tenant.id = :tenantId AND e.estado = :estado "
			+ "AND e.alta >= :desde")
	long contarPorEstadoDesde(@Param("tenantId") String tenantId, @Param("estado") EstadoEjecucion estado,
			@Param("desde") Instant desde);
}
