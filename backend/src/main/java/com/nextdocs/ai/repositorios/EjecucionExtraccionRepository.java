package com.nextdocs.ai.repositorios;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.EjecucionExtraccion;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
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

	@Query("SELECT COALESCE(SUM(e.costo), 0), COALESCE(SUM(e.tokensEntrada), 0), COALESCE(SUM(e.tokensSalida), 0), "
			+ "COUNT(e), COALESCE(SUM(e.duracionMilisegundos), 0), "
			+ "SUM(CASE WHEN e.estado = com.nextdocs.ai.enumeraciones.EstadoEjecucion.COMPLETADA THEN 1 ELSE 0 END), "
			+ "SUM(CASE WHEN e.estado = com.nextdocs.ai.enumeraciones.EstadoEjecucion.FALLIDA THEN 1 ELSE 0 END) "
			+ "FROM EjecucionExtraccion e WHERE e.tenant.id = :tenantId AND e.alta >= :desde AND e.alta < :hasta")
	List<Object[]> totalesEntre(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta);

	@Query("SELECT COUNT(DISTINCT e.documento.id) FROM EjecucionExtraccion e WHERE e.tenant.id = :tenantId "
			+ "AND e.alta >= :desde AND e.alta < :hasta "
			+ "AND e.estado = com.nextdocs.ai.enumeraciones.EstadoEjecucion.COMPLETADA "
			+ "AND e.documento.baja IS NULL AND e.documento.estado IN :estados")
	long contarDocumentosCorrectosEntre(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta, @Param("estados") List<EstadoDocumento> estados);

	@Query("SELECT e.proveedor, COALESCE(SUM(e.costo), 0), COUNT(e) FROM EjecucionExtraccion e "
			+ "WHERE e.tenant.id = :tenantId AND e.alta >= :desde AND e.alta < :hasta GROUP BY e.proveedor "
			+ "ORDER BY SUM(e.costo) DESC")
	List<Object[]> totalesPorProveedor(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta);

	@Query("SELECT COALESCE(e.versionPlantilla.plantilla.codigo, 'SIN_PLANTILLA'), COALESCE(SUM(e.costo), 0), "
			+ "COUNT(e), COUNT(DISTINCT CASE WHEN e.documento.estado IN :estados "
			+ "AND e.estado = com.nextdocs.ai.enumeraciones.EstadoEjecucion.COMPLETADA "
			+ "THEN e.documento.id ELSE NULL END) FROM EjecucionExtraccion e "
			+ "WHERE e.tenant.id = :tenantId AND e.alta >= :desde AND e.alta < :hasta "
			+ "GROUP BY e.versionPlantilla.plantilla.codigo ORDER BY SUM(e.costo) DESC")
	List<Object[]> totalesPorPlantilla(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta, @Param("estados") List<EstadoDocumento> estados);
}
