package com.nextdocs.ai.repositorios;

import java.time.Instant;
import java.util.List;

import com.nextdocs.ai.entidades.EventoSalida;
import com.nextdocs.ai.enumeraciones.EstadoEventoSalida;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoSalidaRepository extends JpaRepository<EventoSalida, String> {

	@Query("SELECT e FROM EventoSalida e WHERE e.estado = :estado AND e.disponibleEn <= :ahora "
			+ "ORDER BY e.disponibleEn")
	List<EventoSalida> listarPendientes(@Param("estado") EstadoEventoSalida estado, @Param("ahora") Instant ahora,
			Pageable paginado);

	@Query("SELECT COUNT(e) FROM EventoSalida e WHERE e.estado = :estado")
	long contarPorEstado(@Param("estado") EstadoEventoSalida estado);
}
