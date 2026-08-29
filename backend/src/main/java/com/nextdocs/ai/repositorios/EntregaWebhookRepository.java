package com.nextdocs.ai.repositorios;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.EntregaWebhook;
import com.nextdocs.ai.enumeraciones.EstadoEntregaWebhook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntregaWebhookRepository extends JpaRepository<EntregaWebhook, String> {

	@Query("SELECT e FROM EntregaWebhook e WHERE e.suscripcion.id = :suscripcionId AND e.eventoId = :eventoId")
	Optional<EntregaWebhook> buscarPorSuscripcionYEvento(@Param("suscripcionId") String suscripcionId,
			@Param("eventoId") String eventoId);

	@Query("SELECT e FROM EntregaWebhook e JOIN FETCH e.suscripcion s JOIN FETCH s.tenant "
			+ "WHERE e.estado = :estado AND e.disponibleEn <= :ahora ORDER BY e.disponibleEn")
	List<EntregaWebhook> listarPendientes(@Param("estado") EstadoEntregaWebhook estado,
			@Param("ahora") Instant ahora, Pageable paginado);

	@Query("SELECT e FROM EntregaWebhook e WHERE e.id = :id AND e.tenant.id = :tenantId")
	Optional<EntregaWebhook> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query(value = "SELECT e FROM EntregaWebhook e WHERE e.tenant.id = :tenantId",
			countQuery = "SELECT COUNT(e) FROM EntregaWebhook e WHERE e.tenant.id = :tenantId")
	Page<EntregaWebhook> listarPorTenant(@Param("tenantId") String tenantId, Pageable paginado);

	@Query(value = "SELECT e FROM EntregaWebhook e WHERE e.tenant.id = :tenantId AND e.estado = :estado",
			countQuery = "SELECT COUNT(e) FROM EntregaWebhook e WHERE e.tenant.id = :tenantId AND e.estado = :estado")
	Page<EntregaWebhook> listarPorTenantYEstado(@Param("tenantId") String tenantId,
			@Param("estado") EstadoEntregaWebhook estado, Pageable paginado);

	@Query(value = "SELECT e FROM EntregaWebhook e WHERE e.suscripcion.id = :suscripcionId",
			countQuery = "SELECT COUNT(e) FROM EntregaWebhook e WHERE e.suscripcion.id = :suscripcionId")
	Page<EntregaWebhook> listarPorSuscripcion(@Param("suscripcionId") String suscripcionId, Pageable paginado);

	@Query("SELECT e.estado, COUNT(e) FROM EntregaWebhook e WHERE e.tenant.id = :tenantId GROUP BY e.estado")
	List<Object[]> contarPorEstado(@Param("tenantId") String tenantId);

	@Query("SELECT COUNT(e) FROM EntregaWebhook e WHERE e.tenant.id = :tenantId "
			+ "AND e.alta >= :desde AND e.alta <= :hasta")
	long contarIntentadas(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta);

	@Query("SELECT COUNT(e) FROM EntregaWebhook e WHERE e.tenant.id = :tenantId "
			+ "AND e.estado = com.nextdocs.ai.enumeraciones.EstadoEntregaWebhook.ENTREGADO "
			+ "AND e.alta >= :desde AND e.alta <= :hasta")
	long contarEntregadas(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta);
}
