package com.nextdocs.ai.repositorios;

import com.nextdocs.ai.entidades.MensajeCorreoSaliente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MensajeCorreoSalienteRepository extends JpaRepository<MensajeCorreoSaliente, String> {

	@Query("SELECT m FROM MensajeCorreoSaliente m WHERE m.tenant.id = :tenantId ORDER BY m.alta DESC")
	Page<MensajeCorreoSaliente> listarPorTenant(@Param("tenantId") String tenantId, Pageable paginado);
}
