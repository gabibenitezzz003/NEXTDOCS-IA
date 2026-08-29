package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.LineaWhatsapp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LineaWhatsappRepository extends JpaRepository<LineaWhatsapp, String> {

	@Query("SELECT l FROM LineaWhatsapp l WHERE l.baja IS NULL AND l.tenant.id = :tenantId ORDER BY l.alta")
	List<LineaWhatsapp> listarPorTenant(@Param("tenantId") String tenantId);

	@Query("SELECT l FROM LineaWhatsapp l WHERE l.baja IS NULL AND l.id = :id AND l.tenant.id = :tenantId")
	Optional<LineaWhatsapp> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query("SELECT l FROM LineaWhatsapp l JOIN FETCH l.tenant WHERE l.baja IS NULL AND l.rutaWebhook = :ruta")
	Optional<LineaWhatsapp> buscarPorRutaWebhook(@Param("ruta") String ruta);

	@Query("SELECT l FROM LineaWhatsapp l WHERE l.baja IS NULL AND l.identificadorNumero = :identificador")
	Optional<LineaWhatsapp> buscarPorIdentificadorNumero(@Param("identificador") String identificador);
}
