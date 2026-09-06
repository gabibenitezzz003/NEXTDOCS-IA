package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.PlantillaDocumental;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlantillaDocumentalRepository extends JpaRepository<PlantillaDocumental, String> {

	@Query("SELECT p FROM PlantillaDocumental p WHERE p.baja IS NULL AND p.tenant.id = :tenantId "
			+ "AND (:familia IS NULL OR p.familia = :familia) ORDER BY p.nombre")
	List<PlantillaDocumental> listarPorTenant(@Param("tenantId") String tenantId, @Param("familia") String familia);

	@Query("SELECT p FROM PlantillaDocumental p WHERE p.baja IS NULL AND p.id = :id AND p.tenant.id = :tenantId")
	Optional<PlantillaDocumental> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query("SELECT p FROM PlantillaDocumental p WHERE p.baja IS NULL AND p.tenant.id = :tenantId "
			+ "AND p.clasificable = TRUE AND p.versionPublicada IS NOT NULL ORDER BY p.codigo")
	List<PlantillaDocumental> listarClasificables(@Param("tenantId") String tenantId);

	@Query("SELECT p FROM PlantillaDocumental p WHERE p.baja IS NULL AND p.tenant.id = :tenantId AND p.codigo = :codigo")
	Optional<PlantillaDocumental> buscarPorCodigo(@Param("tenantId") String tenantId, @Param("codigo") String codigo);

	@Query("SELECT DISTINCT p.familia FROM PlantillaDocumental p WHERE p.baja IS NULL AND p.tenant.id = :tenantId "
			+ "AND p.familia IS NOT NULL ORDER BY p.familia")
	List<String> listarFamilias(@Param("tenantId") String tenantId);
}
