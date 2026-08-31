package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.ValorExtraido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ValorExtraidoRepository extends JpaRepository<ValorExtraido, String> {

	@Query("SELECT v FROM ValorExtraido v WHERE v.ejecucion.id = :ejecucionId ORDER BY v.claveCampo")
	List<ValorExtraido> listarPorEjecucion(@Param("ejecucionId") String ejecucionId);

	@Query("SELECT v FROM ValorExtraido v WHERE v.documento.id = :documentoId AND v.ejecucion.id = "
			+ "(SELECT MAX(e.id) FROM EjecucionExtraccion e WHERE e.documento.id = :documentoId "
			+ "AND e.estado = com.nextdocs.ai.enumeraciones.EstadoEjecucion.COMPLETADA) ORDER BY v.claveCampo")
	List<ValorExtraido> listarUltimosPorDocumento(@Param("documentoId") String documentoId);

	@Query("SELECT v FROM ValorExtraido v WHERE v.ejecucion.id = :ejecucionId AND v.claveCampo = :clave")
	Optional<ValorExtraido> buscarPorClave(@Param("ejecucionId") String ejecucionId, @Param("clave") String clave);

	@Query("SELECT v FROM ValorExtraido v WHERE v.tenant.id = :tenantId AND v.claveCampo = :clave "
			+ "AND v.valorNormalizado = :valor AND v.documento.id <> :documentoId")
	List<ValorExtraido> buscarDuplicados(@Param("tenantId") String tenantId, @Param("clave") String clave,
			@Param("valor") String valor, @Param("documentoId") String documentoId);

	@Query("SELECT v FROM ValorExtraido v WHERE v.documento.id = :documentoId ORDER BY v.claveCampo")
	List<ValorExtraido> listarPorDocumento(@Param("documentoId") String documentoId);

	@Query("SELECT p.codigo, v.presencia, COUNT(v) FROM ValorExtraido v JOIN v.documento d JOIN d.plantilla p "
			+ "WHERE v.tenant.id = :tenantId AND d.recibido >= :desde AND d.recibido <= :hasta "
			+ "GROUP BY p.codigo, v.presencia")
	List<Object[]> agruparPresenciaPorPlantilla(@Param("tenantId") String tenantId,
			@Param("desde") java.time.Instant desde, @Param("hasta") java.time.Instant hasta);
}
