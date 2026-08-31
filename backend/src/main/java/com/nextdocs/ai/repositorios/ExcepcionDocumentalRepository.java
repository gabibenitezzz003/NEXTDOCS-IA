package com.nextdocs.ai.repositorios;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.ExcepcionDocumental;
import com.nextdocs.ai.enumeraciones.EstadoExcepcion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExcepcionDocumentalRepository extends JpaRepository<ExcepcionDocumental, String> {

	Optional<ExcepcionDocumental> findByClaveDeduplicacion(String claveDeduplicacion);

	@Query("SELECT e FROM ExcepcionDocumental e WHERE e.baja IS NULL AND e.id = :id AND e.tenant.id = :tenantId")
	Optional<ExcepcionDocumental> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query("SELECT e FROM ExcepcionDocumental e WHERE e.baja IS NULL AND e.tenant.id = :tenantId "
			+ "AND (:estado IS NULL OR e.estado = :estado) ORDER BY e.prioridad DESC, e.alta")
	Page<ExcepcionDocumental> listarPorTenant(@Param("tenantId") String tenantId,
			@Param("estado") EstadoExcepcion estado, Pageable paginado);

	@Query("SELECT e FROM ExcepcionDocumental e WHERE e.baja IS NULL AND e.documento.id = :documentoId "
			+ "ORDER BY e.alta DESC")
	List<ExcepcionDocumental> listarPorDocumento(@Param("documentoId") String documentoId);

	@Query("SELECT e FROM ExcepcionDocumental e WHERE e.baja IS NULL AND e.venceEn IS NOT NULL "
			+ "AND e.venceEn < :ahora AND e.estado IN (com.nextdocs.ai.enumeraciones.EstadoExcepcion.ABIERTA, "
			+ "com.nextdocs.ai.enumeraciones.EstadoExcepcion.EN_CURSO)")
	List<ExcepcionDocumental> listarVencidas(@Param("ahora") Instant ahora, Pageable paginado);

	@Query("SELECT COUNT(e) FROM ExcepcionDocumental e WHERE e.baja IS NULL AND e.tenant.id = :tenantId "
			+ "AND e.estado = :estado")
	long contarPorEstado(@Param("tenantId") String tenantId, @Param("estado") EstadoExcepcion estado);

	@Query("SELECT COUNT(e) FROM ExcepcionDocumental e WHERE e.baja IS NULL AND e.tenant.id = :tenantId "
			+ "AND e.estado <> com.nextdocs.ai.enumeraciones.EstadoExcepcion.RESUELTA "
			+ "AND e.venceEn IS NOT NULL AND e.venceEn < :ahora")
	long contarAbiertasVencidas(@Param("tenantId") String tenantId, @Param("ahora") Instant ahora);

	@Query("SELECT COUNT(e) FROM ExcepcionDocumental e WHERE e.baja IS NULL AND e.tenant.id = :tenantId "
			+ "AND e.resuelta IS NOT NULL AND e.resuelta >= :desde AND e.resuelta <= :hasta")
	long contarResueltasEntre(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta);

	@Query("SELECT COUNT(e) FROM ExcepcionDocumental e WHERE e.baja IS NULL AND e.tenant.id = :tenantId "
			+ "AND e.resuelta IS NOT NULL AND e.resuelta >= :desde AND e.resuelta <= :hasta "
			+ "AND (e.venceEn IS NULL OR e.resuelta <= e.venceEn)")
	long contarResueltasDentroDeSla(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta);

	@Query("SELECT p.codigo, COUNT(DISTINCT d.id) FROM ExcepcionDocumental e JOIN e.documento d JOIN d.plantilla p "
			+ "WHERE e.baja IS NULL AND e.tenant.id = :tenantId AND d.baja IS NULL "
			+ "AND e.estado <> com.nextdocs.ai.enumeraciones.EstadoExcepcion.RESUELTA "
			+ "AND d.recibido >= :desde AND d.recibido <= :hasta GROUP BY p.codigo")
	List<Object[]> agruparDocumentosConAbiertasPorPlantilla(@Param("tenantId") String tenantId,
			@Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
