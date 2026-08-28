package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.nextdocs.ai.convertidores.OriginalFisicoConverter;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.SeguimientoOriginalFisico;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoOriginalFisico;
import com.nextdocs.ai.enumeraciones.PoliticaOriginalFisico;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.OriginalFisicoModel;
import com.nextdocs.ai.modelos.OriginalFisicoReqModel;
import com.nextdocs.ai.repositorios.SeguimientoOriginalFisicoRepository;
import com.nextdocs.ai.utiles.MaquinaEstadoOriginalFisico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OriginalFisicoService {

	public static final String ENTIDAD = "SeguimientoOriginalFisico";

	private static final Logger log = LoggerFactory.getLogger(OriginalFisicoService.class);

	private final SeguimientoOriginalFisicoRepository seguimientoOriginalFisicoRepository;

	private final AuditoriaService auditoriaService;

	private final OriginalFisicoConverter originalFisicoConverter;

	public OriginalFisicoService(SeguimientoOriginalFisicoRepository seguimientoOriginalFisicoRepository,
			AuditoriaService auditoriaService, OriginalFisicoConverter originalFisicoConverter) {
		this.seguimientoOriginalFisicoRepository = seguimientoOriginalFisicoRepository;
		this.auditoriaService = auditoriaService;
		this.originalFisicoConverter = originalFisicoConverter;
	}

	@Transactional
	public Optional<SeguimientoOriginalFisico> iniciarSiCorresponde(Documento documento) {
		VersionPlantilla version = documento.getVersionPlantilla();
		if (version == null || version.getPoliticaOriginalFisico() == null
				|| version.getPoliticaOriginalFisico() == PoliticaOriginalFisico.NO_REQUIERE) {
			return Optional.empty();
		}
		Optional<SeguimientoOriginalFisico> existente = seguimientoOriginalFisicoRepository
				.buscarPorDocumento(documento.getId());
		if (existente.isPresent()) {
			return existente;
		}
		SeguimientoOriginalFisico seguimiento = new SeguimientoOriginalFisico();
		seguimiento.setTenant(documento.getTenant());
		seguimiento.setDocumento(documento);
		seguimiento.setEstado(EstadoOriginalFisico.PENDIENTE);
		seguimiento.setPolitica(version.getPoliticaOriginalFisico());
		seguimiento.setAlta(Instant.now());
		seguimientoOriginalFisicoRepository.save(seguimiento);
		log.info("El documento {} queda con original fisico PENDIENTE por politica {}", documento.getId(),
				version.getPoliticaOriginalFisico());
		return Optional.of(seguimiento);
	}

	public void exigirParaCierre(Documento documento) {
		Optional<SeguimientoOriginalFisico> seguimiento = seguimientoOriginalFisicoRepository
				.buscarPorDocumento(documento.getId());
		if (seguimiento.isEmpty()) {
			return;
		}
		SeguimientoOriginalFisico actual = seguimiento.get();
		if (actual.getPolitica() != PoliticaOriginalFisico.REQUIERE_PARA_CIERRE) {
			return;
		}
		if (!MaquinaEstadoOriginalFisico.estaEnPoder(actual.getEstado())) {
			throw new ValidacionException(
					"La plantilla exige el original fisico para cerrar y su estado es " + actual.getEstado());
		}
	}

	@Transactional
	public OriginalFisicoModel registrarRecepcion(String tenantId, String documentoId, Usuario actor,
			OriginalFisicoReqModel datos) {
		SeguimientoOriginalFisico seguimiento = buscarEntidad(tenantId, documentoId);
		MaquinaEstadoOriginalFisico.validar(seguimiento.getEstado(), EstadoOriginalFisico.RECIBIDO);
		seguimiento.setEstado(EstadoOriginalFisico.RECIBIDO);
		seguimiento.setRecibidoPor(actor);
		seguimiento.setRegistradoPor(actor);
		seguimiento.setRecibido(Instant.now());
		aplicarDatos(seguimiento, datos);
		return guardarYAuditar(tenantId, seguimiento, EstadoOriginalFisico.RECIBIDO);
	}

	@Transactional
	public OriginalFisicoModel registrarArchivado(String tenantId, String documentoId, Usuario actor,
			OriginalFisicoReqModel datos) {
		SeguimientoOriginalFisico seguimiento = buscarEntidad(tenantId, documentoId);
		MaquinaEstadoOriginalFisico.validar(seguimiento.getEstado(), EstadoOriginalFisico.ARCHIVADO);
		seguimiento.setEstado(EstadoOriginalFisico.ARCHIVADO);
		seguimiento.setRegistradoPor(actor);
		seguimiento.setArchivado(Instant.now());
		aplicarDatos(seguimiento, datos);
		return guardarYAuditar(tenantId, seguimiento, EstadoOriginalFisico.ARCHIVADO);
	}

	@Transactional
	public OriginalFisicoModel registrarExtravio(String tenantId, String documentoId, Usuario actor,
			OriginalFisicoReqModel datos) {
		if (datos.getObservacion() == null || datos.getObservacion().isBlank()) {
			throw new ValidacionException("Declarar un original fisico extraviado requiere una observacion");
		}
		SeguimientoOriginalFisico seguimiento = buscarEntidad(tenantId, documentoId);
		MaquinaEstadoOriginalFisico.validar(seguimiento.getEstado(), EstadoOriginalFisico.EXTRAVIADO);
		seguimiento.setEstado(EstadoOriginalFisico.EXTRAVIADO);
		seguimiento.setRegistradoPor(actor);
		seguimiento.setExtraviado(Instant.now());
		aplicarDatos(seguimiento, datos);
		return guardarYAuditar(tenantId, seguimiento, EstadoOriginalFisico.EXTRAVIADO);
	}

	@Transactional(readOnly = true)
	public Optional<OriginalFisicoModel> buscarPorDocumento(String tenantId, String documentoId) {
		return seguimientoOriginalFisicoRepository.buscarPorDocumento(documentoId)
				.filter(seguimiento -> seguimiento.getTenant().getId().equals(tenantId))
				.map(originalFisicoConverter::aModelo);
	}

	@Transactional(readOnly = true)
	public List<OriginalFisicoModel> listarPorEstado(String tenantId, EstadoOriginalFisico estado) {
		return originalFisicoConverter.aModelos(seguimientoOriginalFisicoRepository.listarPorEstado(tenantId,
				estado == null ? EstadoOriginalFisico.PENDIENTE : estado));
	}

	private OriginalFisicoModel guardarYAuditar(String tenantId, SeguimientoOriginalFisico seguimiento,
			EstadoOriginalFisico destino) {
		seguimientoOriginalFisicoRepository.save(seguimiento);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.REVISION_REGISTRADA, ENTIDAD,
				seguimiento.getId(), Map.of("documentoId", seguimiento.getDocumento().getId(), "estado", destino,
						"ubicacion", seguimiento.getUbicacion() == null ? "" : seguimiento.getUbicacion()));
		return originalFisicoConverter.aModelo(seguimiento);
	}

	private void aplicarDatos(SeguimientoOriginalFisico seguimiento, OriginalFisicoReqModel datos) {
		if (datos == null) {
			return;
		}
		if (datos.getUbicacion() != null) {
			seguimiento.setUbicacion(datos.getUbicacion());
		}
		if (datos.getReferenciaFisica() != null) {
			seguimiento.setReferenciaFisica(datos.getReferenciaFisica());
		}
		if (datos.getObservacion() != null) {
			seguimiento.setObservacion(datos.getObservacion());
		}
	}

	private SeguimientoOriginalFisico buscarEntidad(String tenantId, String documentoId) {
		return seguimientoOriginalFisicoRepository.buscarPorDocumento(documentoId)
				.filter(seguimiento -> seguimiento.getTenant().getId().equals(tenantId))
				.orElseThrow(() -> new EntidadNoEncontradaException(
						"El documento " + documentoId + " no tiene seguimiento de original fisico"));
	}
}
