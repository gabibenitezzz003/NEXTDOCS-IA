package com.nextdocs.ai.repositorios;

import java.time.Instant;
import java.util.Optional;

import com.nextdocs.ai.entidades.MensajeWhatsappEntrante;
import com.nextdocs.ai.enumeraciones.ResultadoMensajeWhatsapp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MensajeWhatsappEntranteRepository extends JpaRepository<MensajeWhatsappEntrante, String> {

	@Query("SELECT m FROM MensajeWhatsappEntrante m WHERE m.linea.id = :lineaId "
			+ "AND m.identificadorMensaje = :identificador")
	Optional<MensajeWhatsappEntrante> buscarPorIdentificador(@Param("lineaId") String lineaId,
			@Param("identificador") String identificador);

	@Query("SELECT MAX(m.recibidoEn) FROM MensajeWhatsappEntrante m WHERE m.linea.id = :lineaId "
			+ "AND m.numeroOrigen = :numero")
	Optional<Instant> ultimoContactoDelNumero(@Param("lineaId") String lineaId, @Param("numero") String numero);

	@Query("SELECT m FROM MensajeWhatsappEntrante m WHERE m.tenant.id = :tenantId "
			+ "AND (:resultado IS NULL OR m.resultado = :resultado) ORDER BY m.alta DESC")
	Page<MensajeWhatsappEntrante> listarPorTenant(@Param("tenantId") String tenantId,
			@Param("resultado") ResultadoMensajeWhatsapp resultado, Pageable paginado);

	@Query("SELECT m FROM MensajeWhatsappEntrante m WHERE m.id = :id AND m.tenant.id = :tenantId")
	Optional<MensajeWhatsappEntrante> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);
}
