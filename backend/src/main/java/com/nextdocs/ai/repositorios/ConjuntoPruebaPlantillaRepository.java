package com.nextdocs.ai.repositorios;

import java.util.Optional;

import com.nextdocs.ai.entidades.ConjuntoPruebaPlantilla;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConjuntoPruebaPlantillaRepository extends JpaRepository<ConjuntoPruebaPlantilla, String> {

	@Query("SELECT c FROM ConjuntoPruebaPlantilla c JOIN FETCH c.plantilla WHERE c.baja IS NULL "
			+ "AND c.plantilla.id = :plantillaId AND c.tenant.id = :tenantId")
	Optional<ConjuntoPruebaPlantilla> buscarActivo(@Param("tenantId") String tenantId,
			@Param("plantillaId") String plantillaId);
}
