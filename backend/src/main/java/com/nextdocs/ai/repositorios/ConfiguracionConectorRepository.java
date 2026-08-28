package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.ConfiguracionConector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConfiguracionConectorRepository extends JpaRepository<ConfiguracionConector, String> {

	@Query("SELECT c FROM ConfiguracionConector c WHERE c.baja IS NULL AND c.tenant.id = :tenantId ORDER BY c.codigo")
	List<ConfiguracionConector> listarPorTenant(@Param("tenantId") String tenantId);

	@Query("SELECT c FROM ConfiguracionConector c WHERE c.baja IS NULL AND c.activo = TRUE "
			+ "AND c.tenant.id = :tenantId ORDER BY c.codigo")
	List<ConfiguracionConector> listarActivos(@Param("tenantId") String tenantId);

	@Query("SELECT c FROM ConfiguracionConector c WHERE c.baja IS NULL AND c.tenant.id = :tenantId "
			+ "AND c.codigo = :codigo")
	Optional<ConfiguracionConector> buscarPorCodigo(@Param("tenantId") String tenantId,
			@Param("codigo") String codigo);
}
