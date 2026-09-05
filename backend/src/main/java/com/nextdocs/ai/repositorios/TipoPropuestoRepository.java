package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.TipoPropuesto;
import com.nextdocs.ai.enumeraciones.EstadoTipoPropuesto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoPropuestoRepository extends JpaRepository<TipoPropuesto, String> {

	@Query("SELECT t FROM TipoPropuesto t WHERE t.tenant.id = :tenantId AND t.codigoSugerido = :codigo")
	Optional<TipoPropuesto> buscarPorCodigo(@Param("tenantId") String tenantId, @Param("codigo") String codigo);

	@Query("SELECT t FROM TipoPropuesto t WHERE t.tenant.id = :tenantId "
			+ "AND (:estado IS NULL OR t.estado = :estado) ORDER BY t.veces DESC, t.alta DESC")
	List<TipoPropuesto> listarPorTenant(@Param("tenantId") String tenantId,
			@Param("estado") EstadoTipoPropuesto estado);

	@Query("SELECT t FROM TipoPropuesto t WHERE t.id = :id AND t.tenant.id = :tenantId")
	Optional<TipoPropuesto> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);
}
