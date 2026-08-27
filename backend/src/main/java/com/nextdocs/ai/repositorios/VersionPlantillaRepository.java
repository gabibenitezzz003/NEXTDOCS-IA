package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.EstadoPlantilla;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VersionPlantillaRepository extends JpaRepository<VersionPlantilla, String> {

	@Query("SELECT v FROM VersionPlantilla v WHERE v.baja IS NULL AND v.plantilla.id = :plantillaId "
			+ "ORDER BY v.numero DESC")
	List<VersionPlantilla> listarPorPlantilla(@Param("plantillaId") String plantillaId);

	@Query("SELECT v FROM VersionPlantilla v WHERE v.baja IS NULL AND v.id = :id AND v.tenant.id = :tenantId")
	Optional<VersionPlantilla> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query("SELECT v FROM VersionPlantilla v WHERE v.baja IS NULL AND v.plantilla.id = :plantillaId "
			+ "AND v.estado = :estado ORDER BY v.numero DESC")
	List<VersionPlantilla> listarPorEstado(@Param("plantillaId") String plantillaId,
			@Param("estado") EstadoPlantilla estado);

	@Query("SELECT COALESCE(MAX(v.numero), 0) FROM VersionPlantilla v WHERE v.plantilla.id = :plantillaId")
	int ultimoNumero(@Param("plantillaId") String plantillaId);
}
