package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.BuzonCorreo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BuzonCorreoRepository extends JpaRepository<BuzonCorreo, String> {

	@Query("SELECT b FROM BuzonCorreo b WHERE b.baja IS NULL AND b.tenant.id = :tenantId ORDER BY b.alta")
	List<BuzonCorreo> listarPorTenant(@Param("tenantId") String tenantId);

	@Query("SELECT b FROM BuzonCorreo b WHERE b.baja IS NULL AND b.id = :id AND b.tenant.id = :tenantId")
	Optional<BuzonCorreo> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query("SELECT b FROM BuzonCorreo b WHERE b.baja IS NULL AND LOWER(b.direccion) = LOWER(:direccion)")
	Optional<BuzonCorreo> buscarPorDireccion(@Param("direccion") String direccion);

	@Query("SELECT b FROM BuzonCorreo b JOIN FETCH b.tenant WHERE b.baja IS NULL AND b.estado = "
			+ "com.nextdocs.ai.enumeraciones.EstadoBuzonCorreo.ACTIVO ORDER BY b.alta")
	List<BuzonCorreo> listarActivos();

	@Query("SELECT b FROM BuzonCorreo b JOIN FETCH b.tenant WHERE b.baja IS NULL AND b.id = :id")
	Optional<BuzonCorreo> buscarConTenant(@Param("id") String id);
}
