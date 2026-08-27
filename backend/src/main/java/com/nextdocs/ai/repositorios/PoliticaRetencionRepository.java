package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.PoliticaRetencion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PoliticaRetencionRepository extends JpaRepository<PoliticaRetencion, String> {

	@Query("SELECT p FROM PoliticaRetencion p WHERE p.baja IS NULL AND p.tenant.id = :tenantId ORDER BY p.clase")
	List<PoliticaRetencion> listarPorTenant(@Param("tenantId") String tenantId);

	@Query("SELECT p FROM PoliticaRetencion p WHERE p.baja IS NULL AND p.activa = TRUE "
			+ "AND p.tenant.id = :tenantId AND p.clase = :clase")
	Optional<PoliticaRetencion> buscarPorClase(@Param("tenantId") String tenantId, @Param("clase") String clase);
}
