package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.convertidores.DocumentoConverter;
import com.nextdocs.ai.convertidores.ExcepcionConverter;
import com.nextdocs.ai.convertidores.ExtraccionConverter;
import com.nextdocs.ai.entidades.ArchivoDocumento;
import com.nextdocs.ai.entidades.CampoPlantilla;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.EjecucionExtraccion;
import com.nextdocs.ai.entidades.EjecucionValidacion;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.OrigenDocumento;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.repositorios.ArchivoDocumentoRepository;
import com.nextdocs.ai.repositorios.CampoPlantillaRepository;
import com.nextdocs.ai.repositorios.CandidatoAsociacionRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.EjecucionExtraccionRepository;
import com.nextdocs.ai.repositorios.EjecucionValidacionRepository;
import com.nextdocs.ai.repositorios.HallazgoValidacionRepository;
import com.nextdocs.ai.repositorios.RevisionDocumentoRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;
import com.nextdocs.ai.specificationBuilder.DocumentoSpecificationBuilder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentoService {

	public static final String ENTIDAD = "Documento";

	private final DocumentoRepository documentoRepository;

	private final ArchivoDocumentoRepository archivoDocumentoRepository;

	private final EjecucionExtraccionRepository ejecucionExtraccionRepository;

	private final EjecucionValidacionRepository ejecucionValidacionRepository;

	private final HallazgoValidacionRepository hallazgoValidacionRepository;

	private final ValorExtraidoRepository valorExtraidoRepository;

	private final CampoPlantillaRepository campoPlantillaRepository;

	private final CandidatoAsociacionRepository candidatoAsociacionRepository;

	private final RevisionDocumentoRepository revisionDocumentoRepository;

	private final AlmacenamientoService almacenamientoService;

	private final ColaExtraccionService colaExtraccionService;

	private final EstadoDocumentalService estadoDocumentalService;

	private final AuditoriaService auditoriaService;

	private final DocumentoConverter documentoConverter;

	private final ExtraccionConverter extraccionConverter;

	private final ExcepcionConverter excepcionConverter;

	private final OriginalFisicoService originalFisicoService;

	public DocumentoService(DocumentoRepository documentoRepository,
			ArchivoDocumentoRepository archivoDocumentoRepository,
			EjecucionExtraccionRepository ejecucionExtraccionRepository,
			EjecucionValidacionRepository ejecucionValidacionRepository,
			HallazgoValidacionRepository hallazgoValidacionRepository,
			ValorExtraidoRepository valorExtraidoRepository, CampoPlantillaRepository campoPlantillaRepository,
			CandidatoAsociacionRepository candidatoAsociacionRepository,
			RevisionDocumentoRepository revisionDocumentoRepository, AlmacenamientoService almacenamientoService,
			ColaExtraccionService colaExtraccionService, EstadoDocumentalService estadoDocumentalService,
			AuditoriaService auditoriaService, DocumentoConverter documentoConverter,
			ExtraccionConverter extraccionConverter, ExcepcionConverter excepcionConverter,
			OriginalFisicoService originalFisicoService) {
		this.documentoRepository = documentoRepository;
		this.archivoDocumentoRepository = archivoDocumentoRepository;
		this.ejecucionExtraccionRepository = ejecucionExtraccionRepository;
		this.ejecucionValidacionRepository = ejecucionValidacionRepository;
		this.hallazgoValidacionRepository = hallazgoValidacionRepository;
		this.valorExtraidoRepository = valorExtraidoRepository;
		this.campoPlantillaRepository = campoPlantillaRepository;
		this.candidatoAsociacionRepository = candidatoAsociacionRepository;
		this.revisionDocumentoRepository = revisionDocumentoRepository;
		this.almacenamientoService = almacenamientoService;
		this.colaExtraccionService = colaExtraccionService;
		this.estadoDocumentalService = estadoDocumentalService;
		this.auditoriaService = auditoriaService;
		this.documentoConverter = documentoConverter;
		this.extraccionConverter = extraccionConverter;
		this.excepcionConverter = excepcionConverter;
		this.originalFisicoService = originalFisicoService;
	}

	@Transactional(readOnly = true)
	public Documento buscarEntidad(String tenantId, String documentoId) {
		return documentoRepository.buscarPorIdYTenant(documentoId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, documentoId));
	}

	@Transactional(readOnly = true)
	public Page<DocumentoModel> listar(String tenantId, List<EstadoDocumento> estados, OrigenDocumento origen,
			String codigoPlantilla, String texto, Instant desde, Instant hasta, Boolean soloRaiz, Pageable paginado) {
		return documentoRepository
				.findAll(DocumentoSpecificationBuilder.construir(tenantId, estados, origen, codigoPlantilla, texto,
						desde, hasta, soloRaiz), paginado)
				.map(documentoConverter::aModelo);
	}

	@Transactional(readOnly = true)
	public DocumentoModel obtener(String tenantId, String documentoId) {
		Documento documento = buscarEntidad(tenantId, documentoId);
		return documentoConverter.aModelo(documento, archivoDocumentoRepository.listarPorDocumento(documentoId));
	}

	@Transactional(readOnly = true)
	public Map<String, Object> detalle(String tenantId, String documentoId) {
		Documento documento = buscarEntidad(tenantId, documentoId);
		Map<String, Object> detalle = new HashMap<>();
		detalle.put("documento",
				documentoConverter.aModelo(documento, archivoDocumentoRepository.listarPorDocumento(documentoId)));
		detalle.put("extraccion", ultimaExtraccion(documento));
		detalle.put("validacion", ultimaValidacion(documentoId));
		detalle.put("candidatos",
				documentoConverter.aModelosCandidatos(candidatoAsociacionRepository.listarPorDocumento(documentoId)));
		detalle.put("revisiones", revisionDocumentoRepository.listarPorDocumento(documentoId).stream()
				.map(revision -> excepcionConverter.aModelo(revision, null)).toList());
		detalle.put("segmentos", documentoConverter.aModelos(documentoRepository.listarSegmentos(documentoId)));
		detalle.put("originalFisico", originalFisicoService.buscarPorDocumento(tenantId, documentoId).orElse(null));
		return detalle;
	}

	@Transactional(readOnly = true)
	public String urlOriginal(String tenantId, String documentoId) {
		buscarEntidad(tenantId, documentoId);
		List<ArchivoDocumento> archivos = archivoDocumentoRepository.listarOriginales(documentoId);
		if (archivos.isEmpty()) {
			throw new EntidadNoEncontradaException("El documento " + documentoId + " no tiene archivo original");
		}
		auditoriaService.registrar(tenantId, AccionAuditoria.DOCUMENTO_DESCARGADO, ENTIDAD, documentoId);
		return almacenamientoService.urlFirmadaDocumento(archivos.get(0).getClaveObjeto());
	}

	@Transactional
	public DocumentoModel reprocesar(String tenantId, String documentoId) {
		Documento documento = buscarEntidad(tenantId, documentoId);
		if (documento.getEstado() == EstadoDocumento.CERRADO) {
			throw new ValidacionException("Un documento cerrado no puede reprocesarse");
		}
		documento.setEstado(EstadoDocumento.RECIBIDO);
		documento.setReintentosExtraccion(0);
		documentoRepository.save(documento);
		colaExtraccionService.encolar(documentoId);
		auditoriaService.registrar(tenantId, AccionAuditoria.DOCUMENTO_REPROCESADO, ENTIDAD, documentoId);
		return documentoConverter.aModelo(documento);
	}

	@Transactional
	public DocumentoModel cerrar(String tenantId, String documentoId) {
		Documento documento = buscarEntidad(tenantId, documentoId);
		originalFisicoService.exigirParaCierre(documento);
		estadoDocumentalService.transicionar(documento, EstadoDocumento.CERRADO);
		return documentoConverter.aModelo(documento);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> resumenPorEstado(String tenantId) {
		Map<String, Object> resumen = new HashMap<>();
		for (EstadoDocumento estado : EstadoDocumento.values()) {
			resumen.put(estado.name(), documentoRepository.contarPorEstado(tenantId, estado));
		}
		resumen.put("profundidadCola", colaExtraccionService.profundidad());
		resumen.put("profundidadReintento", colaExtraccionService.profundidadReintento());
		return resumen;
	}

	private Object ultimaExtraccion(Documento documento) {
		List<EjecucionExtraccion> ejecuciones = ejecucionExtraccionRepository.listarPorDocumento(documento.getId());
		if (ejecuciones.isEmpty()) {
			return null;
		}
		EjecucionExtraccion ultima = ejecuciones.get(0);
		Map<String, CampoPlantilla> camposPorClave = new HashMap<>();
		if (documento.getVersionPlantilla() != null) {
			for (CampoPlantilla campo : campoPlantillaRepository
					.listarPorVersion(documento.getVersionPlantilla().getId())) {
				camposPorClave.put(campo.getClave(), campo);
			}
		}
		return extraccionConverter.aModelo(ultima, valorExtraidoRepository.listarPorEjecucion(ultima.getId()),
				camposPorClave);
	}

	private Object ultimaValidacion(String documentoId) {
		List<EjecucionValidacion> validaciones = ejecucionValidacionRepository.listarPorDocumento(documentoId);
		if (validaciones.isEmpty()) {
			return null;
		}
		EjecucionValidacion ultima = validaciones.get(0);
		return extraccionConverter.aModelo(ultima, hallazgoValidacionRepository.listarPorEjecucion(ultima.getId()));
	}
}
