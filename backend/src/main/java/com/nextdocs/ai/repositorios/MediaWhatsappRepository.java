package com.nextdocs.ai.repositorios;

import java.util.List;

import com.nextdocs.ai.entidades.MediaWhatsapp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaWhatsappRepository extends JpaRepository<MediaWhatsapp, String> {

	@Query("SELECT m FROM MediaWhatsapp m WHERE m.mensaje.id = :mensajeId ORDER BY m.alta")
	List<MediaWhatsapp> listarPorMensaje(@Param("mensajeId") String mensajeId);
}
