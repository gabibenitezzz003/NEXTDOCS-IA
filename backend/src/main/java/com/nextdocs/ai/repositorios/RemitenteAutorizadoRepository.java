package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.RemitenteAutorizado;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RemitenteAutorizadoRepository extends JpaRepository<RemitenteAutorizado, String> {

	@Query("SELECT r FROM RemitenteAutorizado r WHERE r.baja IS NULL AND r.buzon.id = :buzonId ORDER BY r.patron")
	List<RemitenteAutorizado> listarPorBuzon(@Param("buzonId") String buzonId);

	@Query("SELECT r FROM RemitenteAutorizado r WHERE r.baja IS NULL AND r.id = :id AND r.tenant.id = :tenantId")
	Optional<RemitenteAutorizado> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query("SELECT r FROM RemitenteAutorizado r WHERE r.baja IS NULL AND r.buzon.id = :buzonId "
			+ "AND LOWER(r.patron) = LOWER(:patron)")
	Optional<RemitenteAutorizado> buscarPorPatron(@Param("buzonId") String buzonId, @Param("patron") String patron);
}
