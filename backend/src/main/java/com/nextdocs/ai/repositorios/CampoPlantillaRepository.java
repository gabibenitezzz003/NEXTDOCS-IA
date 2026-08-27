package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.CampoPlantilla;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CampoPlantillaRepository extends JpaRepository<CampoPlantilla, String> {

	@Query("SELECT c FROM CampoPlantilla c WHERE c.baja IS NULL AND c.versionPlantilla.id = :versionId "
			+ "ORDER BY c.orden, c.clave")
	List<CampoPlantilla> listarPorVersion(@Param("versionId") String versionId);

	@Query("SELECT c FROM CampoPlantilla c WHERE c.baja IS NULL AND c.versionPlantilla.id = :versionId "
			+ "AND c.clave = :clave")
	Optional<CampoPlantilla> buscarPorClave(@Param("versionId") String versionId, @Param("clave") String clave);
}
