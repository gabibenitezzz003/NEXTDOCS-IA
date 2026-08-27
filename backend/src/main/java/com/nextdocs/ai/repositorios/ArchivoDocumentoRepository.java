package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.ArchivoDocumento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArchivoDocumentoRepository extends JpaRepository<ArchivoDocumento, String> {

	@Query("SELECT a FROM ArchivoDocumento a WHERE a.baja IS NULL AND a.documento.id = :documentoId "
			+ "ORDER BY a.version DESC")
	List<ArchivoDocumento> listarPorDocumento(@Param("documentoId") String documentoId);

	@Query("SELECT a FROM ArchivoDocumento a WHERE a.baja IS NULL AND a.documento.id = :documentoId "
			+ "AND a.original = TRUE ORDER BY a.version DESC")
	List<ArchivoDocumento> listarOriginales(@Param("documentoId") String documentoId);

	@Query("SELECT a FROM ArchivoDocumento a WHERE a.baja IS NULL AND a.id = :id AND a.tenant.id = :tenantId")
	Optional<ArchivoDocumento> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query("SELECT COALESCE(SUM(a.tamano), 0) FROM ArchivoDocumento a WHERE a.baja IS NULL AND a.tenant.id = :tenantId")
	long sumarTamanoPorTenant(@Param("tenantId") String tenantId);
}
