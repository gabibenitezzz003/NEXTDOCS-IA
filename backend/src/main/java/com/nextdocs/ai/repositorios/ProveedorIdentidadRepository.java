package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.ProveedorIdentidad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProveedorIdentidadRepository extends JpaRepository<ProveedorIdentidad, String> {

	@Query("SELECT p FROM ProveedorIdentidad p WHERE p.baja IS NULL AND p.tenant.id = :tenantId ORDER BY p.codigo")
	List<ProveedorIdentidad> listarPorTenant(@Param("tenantId") String tenantId);

	@Query("SELECT p FROM ProveedorIdentidad p WHERE p.baja IS NULL AND p.id = :id AND p.tenant.id = :tenantId")
	Optional<ProveedorIdentidad> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query("SELECT p FROM ProveedorIdentidad p JOIN FETCH p.tenant t WHERE p.baja IS NULL "
			+ "AND t.codigo = :codigoTenant AND p.codigo = :codigo")
	Optional<ProveedorIdentidad> buscarPorCodigoTenantYCodigo(@Param("codigoTenant") String codigoTenant,
			@Param("codigo") String codigo);

	@Query("SELECT p FROM ProveedorIdentidad p WHERE p.baja IS NULL AND p.tenant.id = :tenantId "
			+ "AND p.codigo = :codigo")
	Optional<ProveedorIdentidad> buscarPorCodigo(@Param("tenantId") String tenantId, @Param("codigo") String codigo);
}
