package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.CandidatoAsociacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CandidatoAsociacionRepository extends JpaRepository<CandidatoAsociacion, String> {

	@Query("SELECT c FROM CandidatoAsociacion c WHERE c.documento.id = :documentoId ORDER BY c.puntaje DESC")
	List<CandidatoAsociacion> listarPorDocumento(@Param("documentoId") String documentoId);

	@Query("SELECT c FROM CandidatoAsociacion c WHERE c.documento.id = :documentoId AND c.seleccionado = TRUE")
	Optional<CandidatoAsociacion> buscarSeleccionado(@Param("documentoId") String documentoId);

	@Query("SELECT c FROM CandidatoAsociacion c WHERE c.id = :id AND c.tenant.id = :tenantId")
	Optional<CandidatoAsociacion> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);
}
