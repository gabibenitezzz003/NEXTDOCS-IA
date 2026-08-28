package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.EstadoUsuario;
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

	@Query("SELECT u FROM Usuario u WHERE u.baja IS NULL AND u.tenant.id = :tenantId "
			+ "AND (:estado IS NULL OR u.estado = :estado) "
			+ "AND (:texto IS NULL OR LOWER(u.email) LIKE :texto OR LOWER(u.nombre) LIKE :texto)")
	Page<Usuario> buscarPorTenant(@Param("tenantId") String tenantId, @Param("estado") EstadoUsuario estado,
			@Param("texto") String texto, Pageable paginado);

	default Page<Usuario> listarFiltrado(String tenantId, EstadoUsuario estado, String texto, Pageable paginado) {
		String patron = texto == null || texto.isBlank() ? null : "%" + texto.toLowerCase() + "%";
		return buscarPorTenant(tenantId, estado, patron, paginado);
	}

	@Query("SELECT COUNT(DISTINCT u) FROM Usuario u JOIN u.roles r JOIN r.permisos p "
			+ "WHERE u.baja IS NULL AND u.tenant.id = :tenantId AND u.estado = :estado AND p = :permiso")
	long contarConPermiso(@Param("tenantId") String tenantId, @Param("estado") EstadoUsuario estado,
			@Param("permiso") String permiso);

	@Query("SELECT COUNT(u) FROM Usuario u JOIN u.roles r WHERE u.baja IS NULL AND r.id = :rolId")
	long contarPorRol(@Param("rolId") String rolId);

	@Query("SELECT COUNT(u) FROM Usuario u WHERE u.baja IS NULL AND u.tenant.id = :tenantId AND u.estado = :estado")
	long contarPorEstado(@Param("tenantId") String tenantId, @Param("estado") EstadoUsuario estado);

	@Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles WHERE u.baja IS NULL AND u.id = :id "
			+ "AND u.estado = com.nextdocs.ai.enumeraciones.EstadoUsuario.ACTIVO")
	Optional<Usuario> buscarHabilitadoConRoles(@Param("id") String id);
}
