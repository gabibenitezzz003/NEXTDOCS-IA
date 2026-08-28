package com.nextdocs.ai.repositorios;

import java.util.Optional;

import com.nextdocs.ai.entidades.PoliticaCostoTenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PoliticaCostoTenantRepository extends JpaRepository<PoliticaCostoTenant, String> {

	@Query("SELECT p FROM PoliticaCostoTenant p WHERE p.tenant.id = :tenantId")
	Optional<PoliticaCostoTenant> buscarPorTenant(@Param("tenantId") String tenantId);
}
