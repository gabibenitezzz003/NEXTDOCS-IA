package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.CorreccionAprendida;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CorreccionAprendidaRepository extends JpaRepository<CorreccionAprendida, String> {

	@Query("SELECT c FROM CorreccionAprendida c WHERE c.tenant.id = :tenantId "
			+ "AND c.plantillaCodigo = :plantilla AND c.emisorClave = :emisor AND c.claveCampo = :campo "
			+ "AND c.valorLeido = :leido")
	Optional<CorreccionAprendida> buscar(@Param("tenantId") String tenantId,
			@Param("plantilla") String plantilla, @Param("emisor") String emisor,
			@Param("campo") String campo, @Param("leido") String leido);

	@Query("SELECT c FROM CorreccionAprendida c WHERE c.tenant.id = :tenantId "
			+ "AND c.plantillaCodigo = :plantilla AND c.emisorClave IN (:emisor, :generico) "
			+ "ORDER BY c.veces DESC, c.actualizado DESC")
	List<CorreccionAprendida> listarPara(@Param("tenantId") String tenantId,
			@Param("plantilla") String plantilla, @Param("emisor") String emisor,
			@Param("generico") String generico);
}
