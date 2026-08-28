package com.nextdocs.ai.repositorios;

import java.util.List;

import com.nextdocs.ai.entidades.ResultadoCasoPrueba;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResultadoCasoPruebaRepository extends JpaRepository<ResultadoCasoPrueba, String> {

	@Query("SELECT r FROM ResultadoCasoPrueba r JOIN FETCH r.caso WHERE r.ejecucion.id = :ejecucionId")
	List<ResultadoCasoPrueba> listarPorEjecucion(@Param("ejecucionId") String ejecucionId);
}
