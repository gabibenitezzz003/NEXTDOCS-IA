package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.OrigenIdentidad;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

	@Query("SELECT u FROM Usuario u WHERE u.baja IS NULL AND u.tenant.id = :tenantId AND LOWER(u.email) = LOWER(:email)")
	Optional<Usuario> buscarPorEmail(@Param("tenantId") String tenantId, @Param("email") String email);

	@Query("SELECT u FROM Usuario u WHERE u.baja IS NULL AND LOWER(u.email) = LOWER(:email)")
	List<Usuario> buscarPorEmailEnCualquierTenant(@Param("email") String email);

	@Query("SELECT u FROM Usuario u WHERE u.baja IS NULL AND u.id = :id AND u.tenant.id = :tenantId")
	Optional<Usuario> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query("SELECT u FROM Usuario u WHERE u.baja IS NULL AND u.tenant.id = :tenantId "
			+ "AND u.origenIdentidad = :origen AND u.idUsuarioExterno = :idExterno")
	Optional<Usuario> buscarPorIdentidadExterna(@Param("tenantId") String tenantId,
			@Param("origen") OrigenIdentidad origen, @Param("idExterno") String idExterno);

	@Query("SELECT u FROM Usuario u WHERE u.baja IS NULL AND u.tenant.id = :tenantId")
	Page<Usuario> listarPorTenant(@Param("tenantId") String tenantId, Pageable paginado);
}
