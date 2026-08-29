package com.nextdocs.ai.repositorios;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.LoteExportacion;
import com.nextdocs.ai.enumeraciones.EstadoLoteExportacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoteExportacionRepository extends JpaRepository<LoteExportacion, String> {

	@Query("SELECT l FROM LoteExportacion l WHERE l.baja IS NULL AND l.tenant.id = :tenantId "
			+ "AND (:estado IS NULL OR l.estado = :estado) ORDER BY l.alta DESC")
	Page<LoteExportacion> listarPorTenant(@Param("tenantId") String tenantId,
			@Param("estado") EstadoLoteExportacion estado, Pageable paginado);

	@Query("SELECT l FROM LoteExportacion l WHERE l.baja IS NULL AND l.id = :id AND l.tenant.id = :tenantId")
	Optional<LoteExportacion> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query("SELECT l FROM LoteExportacion l WHERE l.baja IS NULL AND l.estado = "
			+ "com.nextdocs.ai.enumeraciones.EstadoLoteExportacion.SOLICITADO ORDER BY l.alta")
	List<LoteExportacion> listarPendientes(Pageable paginado);

	@Query("SELECT l FROM LoteExportacion l WHERE l.baja IS NULL AND l.estado = "
			+ "com.nextdocs.ai.enumeraciones.EstadoLoteExportacion.DISPONIBLE AND l.venceEn <= :ahora")
	List<LoteExportacion> listarVencidos(@Param("ahora") Instant ahora);

	@Query("SELECT l FROM LoteExportacion l WHERE l.baja IS NULL AND l.estado = "
			+ "com.nextdocs.ai.enumeraciones.EstadoLoteExportacion.DISPONIBLE AND l.avisoVencimiento IS NULL "
			+ "AND l.venceEn <= :limite AND l.venceEn > :ahora")
	List<LoteExportacion> listarPorVencer(@Param("ahora") Instant ahora, @Param("limite") Instant limite);
}
