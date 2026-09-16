package com.nextdocs.ai.repositorios;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentoRepository extends JpaRepository<Documento, String>, JpaSpecificationExecutor<Documento> {

	@Query("SELECT d FROM Documento d WHERE d.baja IS NULL AND d.id = :id AND d.tenant.id = :tenantId")
	Optional<Documento> buscarPorIdYTenant(@Param("id") String id, @Param("tenantId") String tenantId);

	@Query("SELECT d FROM Documento d WHERE d.tenant.id = :tenantId AND d.claveIdempotencia = :clave")
	Optional<Documento> buscarPorClaveIdempotencia(@Param("tenantId") String tenantId,
			@Param("clave") String clave);

	@Query("SELECT d FROM Documento d WHERE d.baja IS NULL AND d.tenant.id = :tenantId "
			+ "AND d.hashContenido = :hash AND d.documentoPadre IS NULL ORDER BY d.alta DESC")
	List<Documento> buscarPorHash(@Param("tenantId") String tenantId, @Param("hash") String hash);

	@Query("SELECT d FROM Documento d WHERE d.baja IS NULL AND d.documentoPadre.id = :padreId ORDER BY d.paginaDesde")
	List<Documento> listarSegmentos(@Param("padreId") String padreId);

	@Query("SELECT COUNT(d) FROM Documento d WHERE d.baja IS NULL AND d.tenant.id = :tenantId AND d.estado = :estado")
	long contarPorEstado(@Param("tenantId") String tenantId, @Param("estado") EstadoDocumento estado);

	@Query("SELECT d FROM Documento d WHERE d.baja IS NULL AND d.estado = :estado AND d.procesado < :limite "
			+ "ORDER BY d.procesado")
	List<Documento> listarEstancadosPorEstado(@Param("estado") EstadoDocumento estado,
			@Param("limite") Instant limite, Pageable paginado);

	@Query("SELECT d FROM Documento d WHERE d.baja IS NULL AND d.estado = com.nextdocs.ai.enumeraciones.EstadoDocumento.RECIBIDO "
			+ "AND d.alta < :limite ORDER BY d.alta")
	List<Documento> listarRecibidosAnteriores(@Param("limite") Instant limite, Pageable paginado);

	@Query("SELECT d FROM Documento d WHERE d.baja IS NULL AND d.retencionAplicada IS NULL "
			+ "AND d.retencionLegal = FALSE AND d.retenerHasta IS NOT NULL AND d.retenerHasta < :ahora "
			+ "ORDER BY d.retenerHasta")
	Page<Documento> listarVencidosPorRetencion(@Param("ahora") Instant ahora, Pageable paginado);

	@Query("SELECT COUNT(d) FROM Documento d WHERE d.baja IS NULL AND d.retencionAplicada IS NULL "
			+ "AND d.retencionLegal = TRUE AND d.retenerHasta IS NOT NULL AND d.retenerHasta < :ahora")
	long contarVencidosConRetencionLegal(@Param("ahora") Instant ahora);

	@Query("SELECT COUNT(d) FROM Documento d WHERE d.baja IS NULL AND d.tenant.id = :tenantId "
			+ "AND d.retencionAplicada IS NULL AND d.retencionLegal = TRUE AND d.retenerHasta IS NOT NULL "
			+ "AND d.retenerHasta < :ahora")
	long contarVencidosConRetencionLegalPorTenant(@Param("tenantId") String tenantId,
			@Param("ahora") Instant ahora);

	@Query("SELECT d FROM Documento d WHERE d.baja IS NULL AND d.tenant.id = :tenantId "
			+ "AND d.retencionAplicada IS NULL AND d.retenerHasta IS NOT NULL AND d.retenerHasta < :ahora "
			+ "ORDER BY d.retenerHasta")
	Page<Documento> listarVencidosPorTenant(@Param("tenantId") String tenantId, @Param("ahora") Instant ahora,
			Pageable paginado);

	@Query("SELECT COUNT(d) FROM Documento d WHERE d.baja IS NULL AND d.tenant.id = :tenantId "
			+ "AND d.retencionLegal = TRUE")
	long contarConRetencionLegal(@Param("tenantId") String tenantId);

	@Query("SELECT COUNT(d) FROM Documento d WHERE d.baja IS NULL AND d.tenant.id = :tenantId "
			+ "AND d.estado = com.nextdocs.ai.enumeraciones.EstadoDocumento.CERRADO AND d.retenerHasta IS NULL")
	long contarCerradosSinPolitica(@Param("tenantId") String tenantId);

	@Query("SELECT COUNT(d) FROM Documento d WHERE d.baja IS NULL AND d.documentoPadre IS NULL "
			+ "AND d.tenant.id = :tenantId AND d.alta >= :desde AND d.alta < :hasta")
	long contarRecibidosEntre(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta);

	@Query("SELECT COUNT(d) FROM Documento d WHERE d.baja IS NULL AND d.tenant.id = :tenantId "
			+ "AND d.cerrado IS NOT NULL AND d.cerrado >= :desde AND d.cerrado <= :hasta")
	long contarCerradosEntre(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta);

	@Query("SELECT COUNT(d) FROM Documento d WHERE d.baja IS NULL AND d.tenant.id = :tenantId "
			+ "AND d.cerrado IS NOT NULL AND d.cerrado >= :desde AND d.cerrado <= :hasta "
			+ "AND NOT EXISTS (SELECT 1 FROM RevisionDocumento r WHERE r.documento = d)")
	long contarCerradosSinIntervencion(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta);

	@Query("SELECT d FROM Documento d WHERE d.baja IS NULL AND d.tenant.id = :tenantId "
			+ "AND d.cerrado IS NOT NULL AND d.recibido IS NOT NULL "
			+ "AND d.cerrado >= :desde AND d.cerrado <= :hasta ORDER BY d.cerrado")
	List<Documento> listarCerradosConCiclo(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta);

	@Query("SELECT COUNT(d) FROM Documento d WHERE d.baja IS NULL AND d.tenant.id = :tenantId "
			+ "AND d.retenerHasta IS NOT NULL AND d.retenerHasta >= :ahora AND d.retenerHasta <= :limite")
	long contarPorVencerEntre(@Param("tenantId") String tenantId, @Param("ahora") Instant ahora,
			@Param("limite") Instant limite);

	@Query("SELECT d.plantilla.codigo, d.plantilla.nombre, d.estado, COUNT(d) FROM Documento d "
			+ "WHERE d.baja IS NULL AND d.tenant.id = :tenantId AND d.plantilla IS NOT NULL "
			+ "AND d.recibido >= :desde AND d.recibido <= :hasta "
			+ "GROUP BY d.plantilla.codigo, d.plantilla.nombre, d.estado")
	List<Object[]> agruparPorPlantillaYEstado(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta);

	@Query("SELECT COUNT(d) FROM Documento d WHERE d.baja IS NULL AND d.documentoPadre IS NULL "
			+ "AND d.tenant.id = :tenantId AND d.recibido >= :desde AND d.recibido <= :hasta")
	long contarRaicesRecibidasEntre(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta);

	@Query("SELECT d FROM Documento d WHERE d.baja IS NULL AND d.documentoPadre IS NULL "
			+ "AND d.tenant.id = :tenantId AND d.recibido >= :desde AND d.recibido <= :hasta "
			+ "ORDER BY d.recibido DESC")
	List<Documento> listarRaicesRecibidasEntre(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta, Pageable paginado);

	@Query("SELECT d FROM Documento d WHERE d.baja IS NULL AND d.tenant.id = :tenantId "
			+ "AND d.cerrado IS NOT NULL AND d.cerrado >= :desde AND d.cerrado <= :hasta ORDER BY d.cerrado DESC")
	List<Documento> listarCerradosEntre(@Param("tenantId") String tenantId, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta, Pageable paginado);
}
