package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.Rol;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolRepository extends JpaRepository<Rol, String> {

	@Query("SELECT r FROM Rol r WHERE r.baja IS NULL AND r.tenant.id = :tenantId ORDER BY r.nombre")
	List<Rol> listarPorTenant(@Param("tenantId") String tenantId);

	@Query("SELECT r FROM Rol r WHERE r.baja IS NULL AND r.tenant.id = :tenantId AND r.codigo = :codigo")
	Optional<Rol> buscarPorCodigo(@Param("tenantId") String tenantId, @Param("codigo") String codigo);

	@Query("SELECT r FROM Rol r WHERE r.baja IS NULL AND r.id = :id AND r.tenant.id = :tenantId")
	Optional<Rol> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);
}
