package com.nextdocs.ai.repositorios;

import java.util.List;

import com.nextdocs.ai.entidades.CambioCampoRevision;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CambioCampoRevisionRepository extends JpaRepository<CambioCampoRevision, String> {

	@Query("SELECT c FROM CambioCampoRevision c WHERE c.revision.id = :revisionId ORDER BY c.claveCampo")
	List<CambioCampoRevision> listarPorRevision(@Param("revisionId") String revisionId);
}
