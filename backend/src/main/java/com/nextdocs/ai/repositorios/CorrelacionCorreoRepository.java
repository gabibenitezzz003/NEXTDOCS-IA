package com.nextdocs.ai.repositorios;

import java.util.Optional;

import com.nextdocs.ai.entidades.CorrelacionCorreo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CorrelacionCorreoRepository extends JpaRepository<CorrelacionCorreo, String> {

	@Query("SELECT c FROM CorrelacionCorreo c JOIN FETCH c.buzon WHERE c.baja IS NULL AND c.token = :token "
			+ "AND c.tenant.id = :tenantId")
	Optional<CorrelacionCorreo> buscarPorTokenYTenant(@Param("token") String token,
			@Param("tenantId") String tenantId);

	@Query("SELECT c FROM CorrelacionCorreo c WHERE c.baja IS NULL AND c.tenant.id = :tenantId ORDER BY c.alta DESC")
	Page<CorrelacionCorreo> listarPorTenant(@Param("tenantId") String tenantId, Pageable paginado);

	@Query("SELECT c FROM CorrelacionCorreo c WHERE c.baja IS NULL AND c.id = :id AND c.tenant.id = :tenantId")
	Optional<CorrelacionCorreo> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);
}
