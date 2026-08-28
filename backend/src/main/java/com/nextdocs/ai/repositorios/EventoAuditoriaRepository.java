package com.nextdocs.ai.repositorios;

import java.time.Instant;
import java.util.List;

import com.nextdocs.ai.entidades.EventoAuditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoAuditoriaRepository
		extends JpaRepository<EventoAuditoria, String>, JpaSpecificationExecutor<EventoAuditoria> {

	Instant INICIO_DE_LOS_TIEMPOS = Instant.EPOCH;

	Instant FIN_DE_LOS_TIEMPOS = Instant.parse("9999-12-31T23:59:59Z");

	@Query("SELECT e FROM EventoAuditoria e WHERE e.tenantId = :tenantId "
			+ "AND e.fecha >= :desde AND e.fecha <= :hasta ORDER BY e.fecha DESC")
	Page<EventoAuditoria> buscarPorRango(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta, Pageable paginado);

	default Page<EventoAuditoria> listarPorTenant(String tenantId, Instant desde, Instant hasta,
			Pageable paginado) {
		return buscarPorRango(tenantId, desde == null ? INICIO_DE_LOS_TIEMPOS : desde,
				hasta == null ? FIN_DE_LOS_TIEMPOS : hasta, paginado);
	}

	@Query("SELECT e FROM EventoAuditoria e WHERE e.tenantId = :tenantId AND e.tipoRecurso = :tipoRecurso "
			+ "AND e.idRecurso = :idRecurso ORDER BY e.fecha DESC")
	List<EventoAuditoria> listarPorRecurso(@Param("tenantId") String tenantId,
			@Param("tipoRecurso") String tipoRecurso, @Param("idRecurso") String idRecurso);

	@Query("SELECT e FROM EventoAuditoria e WHERE e.correlacionId = :correlacionId ORDER BY e.fecha")
	List<EventoAuditoria> listarPorCorrelacion(@Param("correlacionId") String correlacionId);

	@Query("SELECT e FROM EventoAuditoria e WHERE e.tenantId = :tenantId AND e.correlacionId = :correlacionId "
			+ "ORDER BY e.fecha")
	List<EventoAuditoria> listarPorCorrelacionYTenant(@Param("tenantId") String tenantId,
			@Param("correlacionId") String correlacionId);

	@Query("SELECT e.accion, COUNT(e) FROM EventoAuditoria e WHERE e.tenantId = :tenantId "
			+ "AND e.fecha >= :desde AND e.fecha <= :hasta GROUP BY e.accion ORDER BY COUNT(e) DESC")
	List<Object[]> contarPorAccion(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta);

	default List<Object[]> resumirPorAccion(String tenantId, Instant desde, Instant hasta) {
		return contarPorAccion(tenantId, desde == null ? INICIO_DE_LOS_TIEMPOS : desde,
				hasta == null ? FIN_DE_LOS_TIEMPOS : hasta);
	}
}
