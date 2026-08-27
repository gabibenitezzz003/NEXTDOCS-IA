package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.SeguimientoOriginalFisico;
import com.nextdocs.ai.enumeraciones.EstadoOriginalFisico;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeguimientoOriginalFisicoRepository extends JpaRepository<SeguimientoOriginalFisico, String> {

	@Query("SELECT s FROM SeguimientoOriginalFisico s WHERE s.baja IS NULL AND s.documento.id = :documentoId")
	Optional<SeguimientoOriginalFisico> buscarPorDocumento(@Param("documentoId") String documentoId);

	@Query("SELECT s FROM SeguimientoOriginalFisico s WHERE s.baja IS NULL AND s.tenant.id = :tenantId "
			+ "AND s.estado = :estado ORDER BY s.alta")
	List<SeguimientoOriginalFisico> listarPorEstado(@Param("tenantId") String tenantId,
			@Param("estado") EstadoOriginalFisico estado);
}
