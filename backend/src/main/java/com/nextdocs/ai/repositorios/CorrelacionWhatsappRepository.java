package com.nextdocs.ai.repositorios;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.CorrelacionWhatsapp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CorrelacionWhatsappRepository extends JpaRepository<CorrelacionWhatsapp, String> {

	@Query("SELECT c FROM CorrelacionWhatsapp c JOIN FETCH c.linea WHERE c.baja IS NULL AND c.token = :token "
			+ "AND c.tenant.id = :tenantId")
	Optional<CorrelacionWhatsapp> buscarPorTokenYTenant(@Param("token") String token,
			@Param("tenantId") String tenantId);

	@Query("SELECT c FROM CorrelacionWhatsapp c JOIN FETCH c.linea WHERE c.baja IS NULL AND c.linea.id = :lineaId "
			+ "AND c.numeroVinculado = :numero AND c.vinculadoEn >= :desde "
			+ "AND (c.venceEn IS NULL OR c.venceEn > :ahora) ORDER BY c.vinculadoEn DESC")
	List<CorrelacionWhatsapp> listarVinculadasAlNumero(@Param("lineaId") String lineaId,
			@Param("numero") String numero, @Param("desde") Instant desde, @Param("ahora") Instant ahora);

	@Query("SELECT c FROM CorrelacionWhatsapp c WHERE c.baja IS NULL AND c.tenant.id = :tenantId ORDER BY c.alta DESC")
	Page<CorrelacionWhatsapp> listarPorTenant(@Param("tenantId") String tenantId, Pageable paginado);

	@Query("SELECT c FROM CorrelacionWhatsapp c WHERE c.baja IS NULL AND c.id = :id AND c.tenant.id = :tenantId")
	Optional<CorrelacionWhatsapp> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);
}
