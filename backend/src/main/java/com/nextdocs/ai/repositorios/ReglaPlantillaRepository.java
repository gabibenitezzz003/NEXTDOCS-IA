package com.nextdocs.ai.repositorios;

import java.util.List;

import com.nextdocs.ai.entidades.ReglaPlantilla;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReglaPlantillaRepository extends JpaRepository<ReglaPlantilla, String> {

	@Query("SELECT r FROM ReglaPlantilla r WHERE r.baja IS NULL AND r.versionPlantilla.id = :versionId "
			+ "ORDER BY r.orden, r.codigo")
	List<ReglaPlantilla> listarPorVersion(@Param("versionId") String versionId);

	@Query("SELECT r FROM ReglaPlantilla r WHERE r.baja IS NULL AND r.activa = TRUE "
			+ "AND r.versionPlantilla.id = :versionId ORDER BY r.orden, r.codigo")
	List<ReglaPlantilla> listarActivasPorVersion(@Param("versionId") String versionId);
}
