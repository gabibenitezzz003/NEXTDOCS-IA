package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.EjecucionPruebaPlantilla;
import com.nextdocs.ai.enumeraciones.ResultadoQualityGate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EjecucionPruebaPlantillaRepository extends JpaRepository<EjecucionPruebaPlantilla, String> {

	@Query("SELECT e FROM EjecucionPruebaPlantilla e WHERE e.versionPlantilla.id = :versionId "
			+ "ORDER BY e.alta DESC")
	List<EjecucionPruebaPlantilla> listarPorVersion(@Param("versionId") String versionId);

	@Query("SELECT e FROM EjecucionPruebaPlantilla e WHERE e.versionPlantilla.id = :versionId "
			+ "AND e.huellaConjunto = :huella AND e.resultado = :resultado ORDER BY e.alta DESC")
	List<EjecucionPruebaPlantilla> listarAprobadas(@Param("versionId") String versionId,
			@Param("huella") String huella, @Param("resultado") ResultadoQualityGate resultado);

	@Query("SELECT e FROM EjecucionPruebaPlantilla e WHERE e.id = :id AND e.tenant.id = :tenantId")
	Optional<EjecucionPruebaPlantilla> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);
}
