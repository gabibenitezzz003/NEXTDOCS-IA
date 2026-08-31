package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.AvisoAlmacenamiento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AvisoAlmacenamientoRepository extends JpaRepository<AvisoAlmacenamiento, String> {

	@Query("SELECT a FROM AvisoAlmacenamiento a WHERE a.tenant.id = :tenantId AND a.umbral = :umbral")
	Optional<AvisoAlmacenamiento> buscar(@Param("tenantId") String tenantId, @Param("umbral") int umbral);

	@Query("SELECT a FROM AvisoAlmacenamiento a WHERE a.tenant.id = :tenantId ORDER BY a.umbral")
	List<AvisoAlmacenamiento> listarPorTenant(@Param("tenantId") String tenantId);

}
