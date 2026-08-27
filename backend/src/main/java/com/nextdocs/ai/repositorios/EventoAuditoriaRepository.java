package com.nextdocs.ai.repositorios;

import java.time.Instant;
import java.util.List;

import com.nextdocs.ai.entidades.EventoAuditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, String> {

	@Query("SELECT e FROM EventoAuditoria e WHERE e.tenantId = :tenantId "
			+ "AND (:desde IS NULL OR e.fecha >= :desde) AND (:hasta IS NULL OR e.fecha <= :hasta) "
			+ "ORDER BY e.fecha DESC")
	Page<EventoAuditoria> listarPorTenant(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta, Pageable paginado);

	@Query("SELECT e FROM EventoAuditoria e WHERE e.tenantId = :tenantId AND e.tipoRecurso = :tipoRecurso "
			+ "AND e.idRecurso = :idRecurso ORDER BY e.fecha DESC")
	List<EventoAuditoria> listarPorRecurso(@Param("tenantId") String tenantId,
			@Param("tipoRecurso") String tipoRecurso, @Param("idRecurso") String idRecurso);

	@Query("SELECT e FROM EventoAuditoria e WHERE e.correlacionId = :correlacionId ORDER BY e.fecha")
	List<EventoAuditoria> listarPorCorrelacion(@Param("correlacionId") String correlacionId);
}
