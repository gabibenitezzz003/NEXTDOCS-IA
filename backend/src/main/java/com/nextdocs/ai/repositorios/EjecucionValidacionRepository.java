package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.EjecucionValidacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EjecucionValidacionRepository extends JpaRepository<EjecucionValidacion, String> {

	@Query("SELECT e FROM EjecucionValidacion e WHERE e.documento.id = :documentoId ORDER BY e.alta DESC")
	List<EjecucionValidacion> listarPorDocumento(@Param("documentoId") String documentoId);

	@Query("SELECT e FROM EjecucionValidacion e WHERE e.id = :id AND e.tenant.id = :tenantId")
	Optional<EjecucionValidacion> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);
}
