package com.nextdocs.ai.repositorios;

import java.util.List;

import com.nextdocs.ai.entidades.RevisionDocumento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RevisionDocumentoRepository extends JpaRepository<RevisionDocumento, String> {

	@Query("SELECT r FROM RevisionDocumento r WHERE r.documento.id = :documentoId ORDER BY r.alta DESC")
	List<RevisionDocumento> listarPorDocumento(@Param("documentoId") String documentoId);
}
