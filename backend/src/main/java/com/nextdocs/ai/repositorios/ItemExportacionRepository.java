package com.nextdocs.ai.repositorios;

import java.util.List;

import com.nextdocs.ai.entidades.ItemExportacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemExportacionRepository extends JpaRepository<ItemExportacion, String> {

	@Query("SELECT i FROM ItemExportacion i WHERE i.lote.id = :loteId ORDER BY i.alta")
	List<ItemExportacion> listarPorLote(@Param("loteId") String loteId);

	@Query("SELECT COUNT(i) FROM ItemExportacion i WHERE i.lote.id = :loteId AND i.motivoOmision IS NULL")
	long contarIncluidos(@Param("loteId") String loteId);
}
