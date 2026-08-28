package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.CasoPruebaPlantilla;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CasoPruebaPlantillaRepository extends JpaRepository<CasoPruebaPlantilla, String> {

	@Query("SELECT c FROM CasoPruebaPlantilla c WHERE c.baja IS NULL AND c.conjunto.id = :conjuntoId "
			+ "ORDER BY c.orden, c.alta")
	List<CasoPruebaPlantilla> listarActivos(@Param("conjuntoId") String conjuntoId);

	@Query("SELECT c FROM CasoPruebaPlantilla c WHERE c.baja IS NULL AND c.id = :id AND c.tenant.id = :tenantId")
	Optional<CasoPruebaPlantilla> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query("SELECT COUNT(c) FROM CasoPruebaPlantilla c WHERE c.baja IS NULL AND c.conjunto.id = :conjuntoId")
	long contarActivos(@Param("conjuntoId") String conjuntoId);
}
