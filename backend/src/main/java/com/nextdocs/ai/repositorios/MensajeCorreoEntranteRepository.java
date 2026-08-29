package com.nextdocs.ai.repositorios;

import java.util.Optional;

import com.nextdocs.ai.entidades.MensajeCorreoEntrante;
import com.nextdocs.ai.enumeraciones.ResultadoMensajeCorreo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MensajeCorreoEntranteRepository extends JpaRepository<MensajeCorreoEntrante, String> {

	@Query("SELECT m FROM MensajeCorreoEntrante m WHERE m.buzon.id = :buzonId "
			+ "AND m.identificadorMensaje = :identificador")
	Optional<MensajeCorreoEntrante> buscarPorIdentificador(@Param("buzonId") String buzonId,
			@Param("identificador") String identificador);

	@Query("SELECT m FROM MensajeCorreoEntrante m WHERE m.tenant.id = :tenantId "
			+ "AND (:resultado IS NULL OR m.resultado = :resultado) ORDER BY m.alta DESC")
	Page<MensajeCorreoEntrante> listarPorTenant(@Param("tenantId") String tenantId,
			@Param("resultado") ResultadoMensajeCorreo resultado, Pageable paginado);

	@Query("SELECT m FROM MensajeCorreoEntrante m WHERE m.id = :id AND m.tenant.id = :tenantId")
	Optional<MensajeCorreoEntrante> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);
}
