package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.SuscripcionWebhook;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SuscripcionWebhookRepository extends JpaRepository<SuscripcionWebhook, String> {

	@Query("SELECT s FROM SuscripcionWebhook s WHERE s.baja IS NULL AND s.tenant.id = :tenantId ORDER BY s.nombre")
	List<SuscripcionWebhook> listarPorTenant(@Param("tenantId") String tenantId);

	@Query("SELECT DISTINCT s FROM SuscripcionWebhook s JOIN FETCH s.tenant JOIN s.eventos e "
			+ "WHERE s.baja IS NULL AND s.activa = TRUE AND s.tenant.id = :tenantId AND e = :evento")
	List<SuscripcionWebhook> listarActivasPorEvento(@Param("tenantId") String tenantId,
			@Param("evento") TipoEventoCanonico evento);

	@Query("SELECT s FROM SuscripcionWebhook s JOIN FETCH s.tenant WHERE s.baja IS NULL AND s.id = :id "
			+ "AND s.tenant.id = :tenantId")
	Optional<SuscripcionWebhook> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query("SELECT COUNT(s) FROM SuscripcionWebhook s WHERE s.baja IS NULL AND s.tenant.id = :tenantId "
			+ "AND s.activa = :activa")
	long contarPorTenantYActiva(@Param("tenantId") String tenantId, @Param("activa") boolean activa);
}
