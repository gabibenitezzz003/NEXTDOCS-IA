package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.convertidores.ExcepcionConverter;
import com.nextdocs.ai.entidades.CambioCampoRevision;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.HallazgoValidacion;
import com.nextdocs.ai.entidades.RevisionDocumento;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.entidades.ValorExtraido;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.DecisionRevision;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.RevisionDocumentoModel;
import com.nextdocs.ai.modelos.RevisionDocumentoReqModel;
import com.nextdocs.ai.repositorios.CambioCampoRevisionRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.HallazgoValidacionRepository;
import com.nextdocs.ai.repositorios.RevisionDocumentoRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;
import com.nextdocs.ai.utiles.ContextoCorrelacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevisionDocumentalService {

	public static final String ENTIDAD = "RevisionDocumento";

	private final RevisionDocumentoRepository revisionDocumentoRepository;

	private final CambioCampoRevisionRepository cambioCampoRevisionRepository;

	private final AprendizajeService aprendizajeService;

	private final ValorExtraidoRepository valorExtraidoRepository;

	private final HallazgoValidacionRepository hallazgoValidacionRepository;

	private final DocumentoRepository documentoRepository;

	private final EstadoDocumentalService estadoDocumentalService;

	private final ColaExtraccionService colaExtraccionService;

	private final AuditoriaService auditoriaService;

	private final ExcepcionConverter excepcionConverter;

	public RevisionDocumentalService(RevisionDocumentoRepository revisionDocumentoRepository,
			CambioCampoRevisionRepository cambioCampoRevisionRepository,
			AprendizajeService aprendizajeService,
			ValorExtraidoRepository valorExtraidoRepository,
			HallazgoValidacionRepository hallazgoValidacionRepository, DocumentoRepository documentoRepository,
			EstadoDocumentalService estadoDocumentalService, ColaExtraccionService colaExtraccionService,
			AuditoriaService auditoriaService, ExcepcionConverter excepcionConverter) {
		this.revisionDocumentoRepository = revisionDocumentoRepository;
		this.cambioCampoRevisionRepository = cambioCampoRevisionRepository;
		this.aprendizajeService = aprendizajeService;
		this.valorExtraidoRepository = valorExtraidoRepository;
		this.hallazgoValidacionRepository = hallazgoValidacionRepository;
		this.documentoRepository = documentoRepository;
		this.estadoDocumentalService = estadoDocumentalService;
		this.colaExtraccionService = colaExtraccionService;
		this.auditoriaService = auditoriaService;
		this.excepcionConverter = excepcionConverter;
	}

	@Transactional
	public RevisionDocumentoModel registrar(String tenantId, String documentoId, Usuario actor,
			RevisionDocumentoReqModel datos) {
		Documento documento = documentoRepository.buscarPorIdYTenant(documentoId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de("Documento", documentoId));
		EstadoDocumento estadoAnterior = documento.getEstado();
		EstadoDocumento estadoNuevo = destinoDe(datos.getDecision());
		exigirMotivo(datos);

		RevisionDocumento revision = new RevisionDocumento();
		revision.setTenant(documento.getTenant());
		revision.setDocumento(documento);
		revision.setActor(actor);
		revision.setDecision(datos.getDecision());
		revision.setEstadoAnterior(estadoAnterior);
		revision.setEstadoNuevo(estadoNuevo);
		revision.setMotivo(datos.getMotivo());
		revision.setCorrelacionId(ContextoCorrelacion.obtenerOGenerar());
		revision.setDuracionRevisionMilisegundos(datos.getDuracionRevisionMilisegundos());
		revision.setAlta(Instant.now());
		revisionDocumentoRepository.save(revision);

		int correcciones = aplicarCorrecciones(documento, revision, datos);
		revision.setCantidadCorrecciones(correcciones);
		revisionDocumentoRepository.save(revision);

		sobreescribirHallazgos(actor, datos);

		if (datos.getDecision() == DecisionRevision.REPROCESAR) {
			documento.setEstado(EstadoDocumento.RECIBIDO);
			documentoRepository.save(documento);
			colaExtraccionService.encolar(documento.getId());
		} else {
			estadoDocumentalService.transicionar(documento, estadoNuevo);
		}

		auditoriaService.registrarConDetalle(documento.getTenant().getId(), AccionAuditoria.REVISION_REGISTRADA,
				ENTIDAD, revision.getId(), Map.of("decision", datos.getDecision(), "estadoAnterior", estadoAnterior,
						"estadoNuevo", estadoNuevo, "correcciones", correcciones));
		return excepcionConverter.aModelo(revision,
				cambioCampoRevisionRepository.listarPorRevision(revision.getId()));
	}

	@Transactional(readOnly = true)
	public List<RevisionDocumento> listarPorDocumento(String documentoId) {
		return revisionDocumentoRepository.listarPorDocumento(documentoId);
	}

	private void exigirMotivo(RevisionDocumentoReqModel datos) {
		boolean requiereMotivo = datos.getDecision() == DecisionRevision.RECHAZAR
				|| datos.getDecision() == DecisionRevision.OBSERVAR
				|| (datos.getHallazgosSobreescritos() != null && !datos.getHallazgosSobreescritos().isEmpty());
		if (requiereMotivo && (datos.getMotivo() == null || datos.getMotivo().isBlank())) {
			throw new ValidacionException("La decision requiere un motivo explicito");
		}
	}

	private EstadoDocumento destinoDe(DecisionRevision decision) {
		return switch (decision) {
			case APROBAR -> EstadoDocumento.APROBADO;
			case RECHAZAR -> EstadoDocumento.RECHAZADO;
			case OBSERVAR, CORREGIR -> EstadoDocumento.OBSERVADO;
			case REPROCESAR -> EstadoDocumento.PROCESANDO;
		};
	}

	private int aplicarCorrecciones(Documento documento, RevisionDocumento revision,
			RevisionDocumentoReqModel datos) {
		if (datos.getCorrecciones() == null || datos.getCorrecciones().isEmpty()) {
			return 0;
		}
		List<ValorExtraido> vigentes = valorExtraidoRepository.listarUltimosPorDocumento(documento.getId());
		List<CambioCampoRevision> cambios = new ArrayList<>();
		int aplicadas = 0;
		for (Map.Entry<String, String> correccion : datos.getCorrecciones().entrySet()) {
			ValorExtraido valor = vigentes.stream()
					.filter(candidato -> candidato.getClaveCampo().equals(correccion.getKey())).findFirst()
					.orElseThrow(() -> new EntidadNoEncontradaException(
							"El campo " + correccion.getKey() + " no existe en la extraccion vigente"));
			String anterior = valor.getValorNormalizado();
			valor.setValorAnterior(anterior);
			valor.setValorNormalizado(correccion.getValue());
			valor.setCorregidoManualmente(true);
			valorExtraidoRepository.save(valor);

			CambioCampoRevision cambio = new CambioCampoRevision();
			cambio.setTenant(documento.getTenant());
			cambio.setRevision(revision);
			cambio.setClaveCampo(correccion.getKey());
			cambio.setValorAnterior(anterior);
			cambio.setValorNuevo(correccion.getValue());
			cambio.setMotivo(datos.getMotivo());
			cambio.setAlta(Instant.now());
			cambios.add(cambio);
			aprendizajeService.registrar(documento, correccion.getKey(), anterior, correccion.getValue());
			aplicadas++;
		}
		cambioCampoRevisionRepository.saveAll(cambios);
		return aplicadas;
	}

	private void sobreescribirHallazgos(Usuario actor, RevisionDocumentoReqModel datos) {
		if (datos.getHallazgosSobreescritos() == null || datos.getHallazgosSobreescritos().isEmpty()) {
			return;
		}
		for (String hallazgoId : datos.getHallazgosSobreescritos()) {
			HallazgoValidacion hallazgo = hallazgoValidacionRepository.findById(hallazgoId)
					.orElseThrow(() -> EntidadNoEncontradaException.de("HallazgoValidacion", hallazgoId));
			hallazgo.setSobreescrito(true);
			hallazgo.setMotivoSobreescritura(datos.getMotivo());
			hallazgo.setSobreescritoPor(actor);
			hallazgoValidacionRepository.save(hallazgo);
		}
	}
}
