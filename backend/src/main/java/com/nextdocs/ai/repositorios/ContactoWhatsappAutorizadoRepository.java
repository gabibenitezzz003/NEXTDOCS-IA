package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.ContactoWhatsappAutorizado;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactoWhatsappAutorizadoRepository extends JpaRepository<ContactoWhatsappAutorizado, String> {

	@Query("SELECT c FROM ContactoWhatsappAutorizado c WHERE c.baja IS NULL AND c.linea.id = :lineaId "
			+ "ORDER BY c.patron")
	List<ContactoWhatsappAutorizado> listarPorLinea(@Param("lineaId") String lineaId);

	@Query("SELECT c FROM ContactoWhatsappAutorizado c WHERE c.baja IS NULL AND c.id = :id "
			+ "AND c.tenant.id = :tenantId")
	Optional<ContactoWhatsappAutorizado> buscarPorIdYTenant(@Param("id") String id,
			@Param("tenantId") String tenantId);

	@Query("SELECT c FROM ContactoWhatsappAutorizado c WHERE c.baja IS NULL AND c.linea.id = :lineaId "
			+ "AND c.patron = :patron")
	Optional<ContactoWhatsappAutorizado> buscarPorPatron(@Param("lineaId") String lineaId,
			@Param("patron") String patron);
}
