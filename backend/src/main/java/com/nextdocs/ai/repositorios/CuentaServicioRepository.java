package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.CuentaServicio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuentaServicioRepository extends JpaRepository<CuentaServicio, String> {

	Optional<CuentaServicio> findByClaveHashAndBajaIsNull(String claveHash);

	@Query("SELECT c FROM CuentaServicio c WHERE c.baja IS NULL AND c.tenant.id = :tenantId ORDER BY c.alta DESC")
	List<CuentaServicio> listarPorTenant(@Param("tenantId") String tenantId);

	@Query("SELECT c FROM CuentaServicio c WHERE c.baja IS NULL AND c.id = :id AND c.tenant.id = :tenantId")
	Optional<CuentaServicio> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);
}
