package com.nextdocs.ai.repositorios;

import com.nextdocs.ai.entidades.MensajeWhatsappSaliente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MensajeWhatsappSalienteRepository extends JpaRepository<MensajeWhatsappSaliente, String> {

	@Query("SELECT m FROM MensajeWhatsappSaliente m WHERE m.tenant.id = :tenantId ORDER BY m.alta DESC")
	Page<MensajeWhatsappSaliente> listarPorTenant(@Param("tenantId") String tenantId, Pageable paginado);
}
