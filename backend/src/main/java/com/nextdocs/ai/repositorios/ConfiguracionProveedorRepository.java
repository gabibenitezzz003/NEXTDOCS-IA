package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.ConfiguracionProveedor;
import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConfiguracionProveedorRepository extends JpaRepository<ConfiguracionProveedor, String> {

	@Query("SELECT c FROM ConfiguracionProveedor c WHERE c.baja IS NULL AND c.tenant.id = :tenantId "
			+ "ORDER BY c.prioridad")
	List<ConfiguracionProveedor> listarPorTenant(@Param("tenantId") String tenantId);

	@Query("SELECT c FROM ConfiguracionProveedor c WHERE c.baja IS NULL AND c.activa = TRUE "
			+ "AND c.tenant.id = :tenantId AND c.respaldo = FALSE ORDER BY c.prioridad")
	List<ConfiguracionProveedor> listarActivasPrincipales(@Param("tenantId") String tenantId);

	@Query("SELECT c FROM ConfiguracionProveedor c WHERE c.baja IS NULL AND c.activa = TRUE "
			+ "AND c.tenant.id = :tenantId AND c.respaldo = TRUE ORDER BY c.prioridad")
	List<ConfiguracionProveedor> listarActivasRespaldo(@Param("tenantId") String tenantId);

	@Query("SELECT c FROM ConfiguracionProveedor c WHERE c.baja IS NULL AND c.tenant.id = :tenantId "
			+ "AND c.proveedor = :proveedor")
	Optional<ConfiguracionProveedor> buscarPorProveedor(@Param("tenantId") String tenantId,
			@Param("proveedor") ProveedorDocumentalIa proveedor);
}
