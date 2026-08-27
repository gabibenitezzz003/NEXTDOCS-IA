package com.nextdocs.ai.repositorios;

import java.util.List;

import com.nextdocs.ai.entidades.SegmentoDocumento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SegmentoDocumentoRepository extends JpaRepository<SegmentoDocumento, String> {

	@Query("SELECT s FROM SegmentoDocumento s WHERE s.documentoPadre.id = :padreId ORDER BY s.orden")
	List<SegmentoDocumento> listarPorPadre(@Param("padreId") String padreId);
}
