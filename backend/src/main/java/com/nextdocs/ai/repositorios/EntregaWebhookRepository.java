package com.nextdocs.ai.repositorios;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.EntregaWebhook;
import com.nextdocs.ai.enumeraciones.EstadoEntregaWebhook;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntregaWebhookRepository extends JpaRepository<EntregaWebhook, String> {

	@Query("SELECT e FROM EntregaWebhook e WHERE e.suscripcion.id = :suscripcionId AND e.eventoId = :eventoId")
	Optional<EntregaWebhook> buscarPorSuscripcionYEvento(@Param("suscripcionId") String suscripcionId,
			@Param("eventoId") String eventoId);

	@Query("SELECT e FROM EntregaWebhook e WHERE e.estado = :estado AND e.disponibleEn <= :ahora "
			+ "ORDER BY e.disponibleEn")
	List<EntregaWebhook> listarPendientes(@Param("estado") EstadoEntregaWebhook estado,
			@Param("ahora") Instant ahora, Pageable paginado);

	@Query("SELECT e FROM EntregaWebhook e WHERE e.tenant.id = :tenantId ORDER BY e.alta DESC")
	List<EntregaWebhook> listarPorTenant(@Param("tenantId") String tenantId, Pageable paginado);
}
