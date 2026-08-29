package com.nextdocs.ai.repositorios;

import java.time.Instant;
import java.util.Optional;

import com.nextdocs.ai.entidades.CodigoEmbed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CodigoEmbedRepository extends JpaRepository<CodigoEmbed, String> {

	@Query("SELECT c FROM CodigoEmbed c JOIN FETCH c.usuario u JOIN FETCH u.tenant WHERE c.codigoHash = :codigoHash")
	Optional<CodigoEmbed> buscarPorHash(@Param("codigoHash") String codigoHash);

	@Modifying
	@Query("DELETE FROM CodigoEmbed c WHERE c.venceEn < :limite")
	int borrarVencidosAntesDe(@Param("limite") Instant limite);
}
