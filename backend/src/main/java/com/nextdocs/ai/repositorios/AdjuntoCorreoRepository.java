package com.nextdocs.ai.repositorios;

import java.util.List;

import com.nextdocs.ai.entidades.AdjuntoCorreo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdjuntoCorreoRepository extends JpaRepository<AdjuntoCorreo, String> {

	@Query("SELECT a FROM AdjuntoCorreo a WHERE a.mensaje.id = :mensajeId ORDER BY a.alta")
	List<AdjuntoCorreo> listarPorMensaje(@Param("mensajeId") String mensajeId);
}
