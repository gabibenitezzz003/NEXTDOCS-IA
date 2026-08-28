package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.convertidores.DocumentoConverter;
import com.nextdocs.ai.convertidores.ExcepcionConverter;
import com.nextdocs.ai.convertidores.ExtraccionConverter;
import com.nextdocs.ai.convertidores.GobernanzaConverter;
import com.nextdocs.ai.convertidores.PlantillaConverter;
import com.nextdocs.ai.entidades.CampoPlantilla;
import com.nextdocs.ai.entidades.CandidatoAsociacion;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.EjecucionExtraccion;
import com.nextdocs.ai.entidades.EjecucionValidacion;
import com.nextdocs.ai.entidades.EventoAuditoria;
import com.nextdocs.ai.entidades.PoliticaRetencion;
import com.nextdocs.ai.entidades.ReglaPlantilla;
import com.nextdocs.ai.entidades.RevisionDocumento;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.EstadoEjecucion;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.RegistroExistenteException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.EjecucionValidacionModel;
import com.nextdocs.ai.modelos.EventoAuditoriaModel;
import com.nextdocs.ai.modelos.FiltroAuditoriaModel;
import com.nextdocs.ai.modelos.PoliticaRetencionModel;
import com.nextdocs.ai.modelos.PoliticaRetencionReqModel;
import com.nextdocs.ai.modelos.TrazabilidadDocumentoModel;
import com.nextdocs.ai.repositorios.ArchivoDocumentoRepository;
import com.nextdocs.ai.repositorios.CambioCampoRevisionRepository;
import com.nextdocs.ai.repositorios.CampoPlantillaRepository;
import com.nextdocs.ai.repositorios.CandidatoAsociacionRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.EjecucionExtraccionRepository;
import com.nextdocs.ai.repositorios.EjecucionValidacionRepository;
import com.nextdocs.ai.repositorios.EventoAuditoriaRepository;
import com.nextdocs.ai.repositorios.HallazgoValidacionRepository;
import com.nextdocs.ai.repositorios.PoliticaRetencionRepository;
import com.nextdocs.ai.repositorios.ReglaPlantillaRepository;
import com.nextdocs.ai.repositorios.RevisionDocumentoRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;
import com.nextdocs.ai.specificationBuilder.EventoAuditoriaSpecificationBuilder;
import com.nextdocs.ai.utiles.ExportadorCsv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GobernanzaService {

	public static final String ENTIDAD_AUDITORIA = "EventoAuditoria";

	public static final String ENTIDAD_POLITICA = "PoliticaRetencion";

	public static final int TAMANO_LOTE_EXPORTACION = 500;

	public static final int EVENTOS_MAXIMOS_EXPORTACION = 50000;

	public static final int EVENTOS_MAXIMOS_TRAZABILIDAD = 500;

	private static final Logger log = LoggerFactory.getLogger(GobernanzaService.class);

	private static final List<String> ENCABEZADOS_EXPORTACION = List.of("fecha", "accion", "tipoActor",
			"descripcionActor", "idActor", "tipoRecurso", "idRecurso", "exitoso", "correlacionId", "hashAntes",
			"hashDespues", "direccionIp", "detalle");

	private final EventoAuditoriaRepository eventoAuditoriaRepository;

	private final PoliticaRetencionRepository politicaRetencionRepository;

	private final DocumentoRepository documentoRepository;

	private final ArchivoDocumentoRepository archivoDocumentoRepository;

	private final EjecucionExtraccionRepository ejecucionExtraccionRepository;

	private final EjecucionValidacionRepository ejecucionValidacionRepository;

	private final HallazgoValidacionRepository hallazgoValidacionRepository;

	private final ValorExtraidoRepository valorExtraidoRepository;

	private final CampoPlantillaRepository campoPlantillaRepository;

	private final ReglaPlantillaRepository reglaPlantillaRepository;

	private final CandidatoAsociacionRepository candidatoAsociacionRepository;

	private final RevisionDocumentoRepository revisionDocumentoRepository;

	private final CambioCampoRevisionRepository cambioCampoRevisionRepository;

	private final AuditoriaService auditoriaService;

	private final GobernanzaConverter gobernanzaConverter;

	private final DocumentoConverter documentoConverter;

	private final ExtraccionConverter extraccionConverter;

	private final ExcepcionConverter excepcionConverter;

	private final PlantillaConverter plantillaConverter;

	public GobernanzaService(EventoAuditoriaRepository eventoAuditoriaRepository,
			PoliticaRetencionRepository politicaRetencionRepository, DocumentoRepository documentoRepository,
			ArchivoDocumentoRepository archivoDocumentoRepository,
			EjecucionExtraccionRepository ejecucionExtraccionRepository,
			EjecucionValidacionRepository ejecucionValidacionRepository,
			HallazgoValidacionRepository hallazgoValidacionRepository,
			ValorExtraidoRepository valorExtraidoRepository, CampoPlantillaRepository campoPlantillaRepository,
			ReglaPlantillaRepository reglaPlantillaRepository,
			CandidatoAsociacionRepository candidatoAsociacionRepository,
			RevisionDocumentoRepository revisionDocumentoRepository,
			CambioCampoRevisionRepository cambioCampoRevisionRepository, AuditoriaService auditoriaService,
			GobernanzaConverter gobernanzaConverter, DocumentoConverter documentoConverter,
			ExtraccionConverter extraccionConverter, ExcepcionConverter excepcionConverter,
			PlantillaConverter plantillaConverter) {
		this.eventoAuditoriaRepository = eventoAuditoriaRepository;
		this.politicaRetencionRepository = politicaRetencionRepository;
		this.documentoRepository = documentoRepository;
		this.archivoDocumentoRepository = archivoDocumentoRepository;
		this.ejecucionExtraccionRepository = ejecucionExtraccionRepository;
		this.ejecucionValidacionRepository = ejecucionValidacionRepository;
		this.hallazgoValidacionRepository = hallazgoValidacionRepository;
		this.valorExtraidoRepository = valorExtraidoRepository;
		this.campoPlantillaRepository = campoPlantillaRepository;
		this.reglaPlantillaRepository = reglaPlantillaRepository;
		this.candidatoAsociacionRepository = candidatoAsociacionRepository;
		this.revisionDocumentoRepository = revisionDocumentoRepository;
		this.cambioCampoRevisionRepository = cambioCampoRevisionRepository;
		this.auditoriaService = auditoriaService;
		this.gobernanzaConverter = gobernanzaConverter;
		this.documentoConverter = documentoConverter;
		this.extraccionConverter = extraccionConverter;
		this.excepcionConverter = excepcionConverter;
		this.plantillaConverter = plantillaConverter;
	}

	@Transactional(readOnly = true)
	public Page<EventoAuditoriaModel> consultar(String tenantId, FiltroAuditoriaModel filtro, Pageable paginado) {
		return eventoAuditoriaRepository
				.findAll(EventoAuditoriaSpecificationBuilder.construir(tenantId, filtro), paginado)
				.map(gobernanzaConverter::aModelo);
	}

	@Transactional(readOnly = true)
	public List<EventoAuditoriaModel> porCorrelacion(String tenantId, String correlacionId) {
		return gobernanzaConverter
				.aModelos(eventoAuditoriaRepository.listarPorCorrelacionYTenant(tenantId, correlacionId));
	}

	@Transactional(readOnly = true)
	public List<EventoAuditoriaModel> porRecurso(String tenantId, String tipoRecurso, String idRecurso) {
		return gobernanzaConverter.aModelos(eventoAuditoriaRepository.listarPorRecurso(tenantId, tipoRecurso,
				idRecurso));
	}

	@Transactional(readOnly = true)
	public Map<String, Object> resumen(String tenantId, Instant desde, Instant hasta) {
		Map<String, Object> resumen = new LinkedHashMap<>();
		Map<String, Long> porAccion = new LinkedHashMap<>();
		long total = 0;
		for (Object[] fila : eventoAuditoriaRepository.resumirPorAccion(tenantId, desde, hasta)) {
			long cantidad = ((Number) fila[1]).longValue();
			porAccion.put(String.valueOf(fila[0]), cantidad);
			total += cantidad;
		}
		resumen.put("total", total);
		resumen.put("porAccion", porAccion);
		resumen.put("desde", desde);
		resumen.put("hasta", hasta);
		return resumen;
	}

	@Transactional(readOnly = true)
	public TrazabilidadDocumentoModel reconstruir(String tenantId, String documentoId) {
		Documento documento = documentoRepository.buscarPorIdYTenant(documentoId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(DocumentoService.ENTIDAD, documentoId));
		TrazabilidadDocumentoModel trazabilidad = new TrazabilidadDocumentoModel();
		trazabilidad.setGenerada(Instant.now());
		trazabilidad.setDocumento(
				documentoConverter.aModelo(documento, archivoDocumentoRepository.listarPorDocumento(documentoId)));
		completarPlantilla(trazabilidad, documento);
		completarExtracciones(trazabilidad, documento);
		completarValidaciones(trazabilidad, documentoId);
		completarAsociacion(trazabilidad, documentoId);
		completarRevisiones(trazabilidad, documentoId);
		trazabilidad.setEventos(gobernanzaConverter.aModelos(eventoAuditoriaRepository
				.listarPorRecurso(tenantId, DocumentoService.ENTIDAD, documentoId).stream()
				.limit(EVENTOS_MAXIMOS_TRAZABILIDAD).toList()));
		evaluarIntegridad(trazabilidad, documento);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.AUDITORIA_CONSULTADA,
				DocumentoService.ENTIDAD, documentoId,
				Map.of("completa", trazabilidad.isCompleta(), "faltantes", trazabilidad.getFaltantes()));
		return trazabilidad;
	}

	@Transactional(readOnly = true)
	public String exportarCsv(String tenantId, FiltroAuditoriaModel filtro) {
		ExportadorCsv exportador = new ExportadorCsv(ENCABEZADOS_EXPORTACION);
		int exportados = 0;
		for (EventoAuditoria evento : recolectar(tenantId, filtro)) {
			exportador.fila(Arrays.asList(evento.getFecha(), evento.getAccion(), evento.getTipoActor(),
					evento.getDescripcionActor(), evento.getIdActor(), evento.getTipoRecurso(),
					evento.getIdRecurso(), evento.isExitoso(), evento.getCorrelacionId(), evento.getHashAntes(),
					evento.getHashDespues(), evento.getDireccionIp(), evento.getDetalle()));
			exportados++;
		}
		registrarExportacion(tenantId, filtro, exportados, "CSV");
		return exportador.contenido();
	}

	@Transactional(readOnly = true)
	public List<EventoAuditoriaModel> exportarJson(String tenantId, FiltroAuditoriaModel filtro) {
		List<EventoAuditoriaModel> eventos = gobernanzaConverter.aModelos(recolectar(tenantId, filtro));
		registrarExportacion(tenantId, filtro, eventos.size(), "JSON");
		return eventos;
	}

	@Transactional(readOnly = true)
	public List<PoliticaRetencionModel> listarPoliticas(String tenantId) {
		return gobernanzaConverter.aModelosPolitica(politicaRetencionRepository.listarPorTenant(tenantId));
	}

	@Transactional
	public PoliticaRetencionModel crearPolitica(Tenant tenant, PoliticaRetencionReqModel datos) {
		politicaRetencionRepository.buscarPorClaseSinImportarEstado(tenant.getId(), datos.getClase())
				.ifPresent(existente -> {
					throw new RegistroExistenteException(
							"Ya existe una politica de retencion para la clase " + datos.getClase());
				});
		PoliticaRetencion politica = new PoliticaRetencion();
		politica.setTenant(tenant);
		politica.setClase(datos.getClase());
		politica.setAlta(Instant.now());
		aplicarDatos(politica, datos);
		politicaRetencionRepository.save(politica);
		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.POLITICA_MODIFICADA, ENTIDAD_POLITICA,
				politica.getId(), Map.of("clase", politica.getClase(), "operacion", "ALTA", "duracionDias",
						politica.getDuracionDias(), "accion", politica.getAccion()));
		return gobernanzaConverter.aModelo(politica);
	}

	@Transactional
	public PoliticaRetencionModel actualizarPolitica(String tenantId, String politicaId,
			PoliticaRetencionReqModel datos) {
		PoliticaRetencion politica = buscarPolitica(tenantId, politicaId);
		Map<String, Object> antes = instantanea(politica);
		aplicarDatos(politica, datos);
		politicaRetencionRepository.save(politica);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.POLITICA_MODIFICADA, ENTIDAD_POLITICA,
				politicaId, Map.of("clase", politica.getClase(), "operacion", "MODIFICACION", "antes", antes,
						"despues", instantanea(politica)));
		return gobernanzaConverter.aModelo(politica);
	}

	@Transactional
	public PoliticaRetencionModel desactivarPolitica(String tenantId, String politicaId) {
		PoliticaRetencion politica = buscarPolitica(tenantId, politicaId);
		if (!politica.isActiva()) {
			throw new ValidacionException("La politica " + politica.getClase() + " ya esta inactiva");
		}
		politica.setActiva(false);
		politicaRetencionRepository.save(politica);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.POLITICA_MODIFICADA, ENTIDAD_POLITICA,
				politicaId, Map.of("clase", politica.getClase(), "operacion", "DESACTIVACION"));
		return gobernanzaConverter.aModelo(politica);
	}

	@Transactional
	public void cambiarRetencionLegal(String tenantId, String documentoId, boolean activa, String motivo,
			Usuario actor) {
		if (motivo == null || motivo.isBlank()) {
			throw new ValidacionException("Cambiar la retencion legal exige un motivo explicito");
		}
		Documento documento = documentoRepository.buscarPorIdYTenant(documentoId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(DocumentoService.ENTIDAD, documentoId));
		if (documento.isRetencionLegal() == activa) {
			throw new ValidacionException(
					"El documento ya tiene la retencion legal en " + (activa ? "activa" : "inactiva"));
		}
		documento.setRetencionLegal(activa);
		documentoRepository.save(documento);
		Map<String, Object> detalle = new HashMap<>();
		detalle.put("retencionLegal", activa);
		detalle.put("motivo", motivo);
		detalle.put("actor", actor == null ? "sistema" : actor.getEmail());
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.RETENCION_LEGAL_MODIFICADA,
				DocumentoService.ENTIDAD, documentoId, detalle);
		log.info("La retencion legal del documento {} queda en {} por motivo: {}", documentoId, activa, motivo);
	}

	private List<EventoAuditoria> recolectar(String tenantId, FiltroAuditoriaModel filtro) {
		List<EventoAuditoria> eventos = new ArrayList<>();
		int pagina = 0;
		Page<EventoAuditoria> lote;
		do {
			lote = eventoAuditoriaRepository.findAll(
					EventoAuditoriaSpecificationBuilder.construir(tenantId, filtro),
					PageRequest.of(pagina, TAMANO_LOTE_EXPORTACION, Sort.by(Sort.Direction.DESC, "fecha")));
			eventos.addAll(lote.getContent());
			pagina++;
		} while (lote.hasNext() && eventos.size() < EVENTOS_MAXIMOS_EXPORTACION);
		if (eventos.size() > EVENTOS_MAXIMOS_EXPORTACION) {
			return eventos.subList(0, EVENTOS_MAXIMOS_EXPORTACION);
		}
		return eventos;
	}

	private void registrarExportacion(String tenantId, FiltroAuditoriaModel filtro, int cantidad, String formato) {
		Map<String, Object> detalle = new HashMap<>();
		detalle.put("formato", formato);
		detalle.put("cantidad", cantidad);
		detalle.put("truncada", cantidad >= EVENTOS_MAXIMOS_EXPORTACION);
		if (filtro != null) {
			detalle.put("desde", String.valueOf(filtro.getDesde()));
			detalle.put("hasta", String.valueOf(filtro.getHasta()));
			detalle.put("accion", String.valueOf(filtro.getAccion()));
			detalle.put("tipoRecurso", String.valueOf(filtro.getTipoRecurso()));
		}
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.AUDITORIA_EXPORTADA, ENTIDAD_AUDITORIA,
				tenantId, detalle);
		log.info("Se exportaron {} eventos de auditoria del tenant {} en formato {}", cantidad, tenantId, formato);
	}

	private void completarPlantilla(TrazabilidadDocumentoModel trazabilidad, Documento documento) {
		if (documento.getPlantilla() != null) {
			trazabilidad.setCodigoPlantilla(documento.getPlantilla().getCodigo());
			trazabilidad.setNombrePlantilla(documento.getPlantilla().getNombre());
		}
		VersionPlantilla version = documento.getVersionPlantilla();
		if (version == null) {
			return;
		}
		trazabilidad.setVersionPlantillaId(version.getId());
		trazabilidad.setNumeroVersionPlantilla(version.getNumero());
		trazabilidad.setVersionPrompt(version.getVersionPrompt());
		trazabilidad.setVersionEsquema(version.getVersionEsquema());
		trazabilidad.setUmbralAutoaprobacion(version.getUmbralAutoaprobacion());
		for (ReglaPlantilla regla : reglaPlantillaRepository.listarPorVersion(version.getId())) {
			trazabilidad.getReglas().add(plantillaConverter.aModelo(regla));
		}
	}

	private void completarExtracciones(TrazabilidadDocumentoModel trazabilidad, Documento documento) {
		Map<String, CampoPlantilla> camposPorClave = new HashMap<>();
		if (documento.getVersionPlantilla() != null) {
			for (CampoPlantilla campo : campoPlantillaRepository
					.listarPorVersion(documento.getVersionPlantilla().getId())) {
				camposPorClave.put(campo.getClave(), campo);
			}
		}
		for (EjecucionExtraccion ejecucion : ejecucionExtraccionRepository.listarPorDocumento(documento.getId())) {
			trazabilidad.getExtracciones().add(extraccionConverter.aModelo(ejecucion,
					valorExtraidoRepository.listarPorEjecucion(ejecucion.getId()), camposPorClave));
			if (trazabilidad.getProveedor() == null && ejecucion.getEstado() == EstadoEjecucion.COMPLETADA) {
				trazabilidad.setProveedor(ejecucion.getProveedor());
				trazabilidad.setModelo(ejecucion.getModelo());
				if (trazabilidad.getVersionPrompt() == null) {
					trazabilidad.setVersionPrompt(ejecucion.getVersionPrompt());
				}
				if (trazabilidad.getVersionEsquema() == null) {
					trazabilidad.setVersionEsquema(ejecucion.getVersionEsquema());
				}
			}
		}
	}

	private void completarValidaciones(TrazabilidadDocumentoModel trazabilidad, String documentoId) {
		for (EjecucionValidacion ejecucion : ejecucionValidacionRepository.listarPorDocumento(documentoId)) {
			trazabilidad.getValidaciones().add(extraccionConverter.aModelo(ejecucion,
					hallazgoValidacionRepository.listarPorEjecucion(ejecucion.getId())));
		}
	}

	private void completarAsociacion(TrazabilidadDocumentoModel trazabilidad, String documentoId) {
		for (CandidatoAsociacion candidato : candidatoAsociacionRepository.listarPorDocumento(documentoId)) {
			trazabilidad.getCandidatos().add(documentoConverter.aModelo(candidato));
			if (candidato.isSeleccionado() && candidato.getReferencia() != null) {
				trazabilidad.setAsociacionSeleccionada(candidato.getReferencia().getTipoObjeto() + ":"
						+ candidato.getReferencia().getIdObjeto());
			}
		}
	}

	private void completarRevisiones(TrazabilidadDocumentoModel trazabilidad, String documentoId) {
		for (RevisionDocumento revision : revisionDocumentoRepository.listarPorDocumento(documentoId)) {
			trazabilidad.getRevisiones().add(excepcionConverter.aModelo(revision,
					cambioCampoRevisionRepository.listarPorRevision(revision.getId())));
			if (trazabilidad.getRevisor() == null && revision.getActor() != null) {
				trazabilidad.setRevisor(revision.getActor().getEmail());
			}
		}
	}

	private void evaluarIntegridad(TrazabilidadDocumentoModel trazabilidad, Documento documento) {
		List<String> faltantes = trazabilidad.getFaltantes();
		if (trazabilidad.getVersionPlantillaId() == null) {
			faltantes.add("versionPlantilla");
		}
		if (trazabilidad.getExtracciones().isEmpty()) {
			faltantes.add("extraccion");
		}
		if (trazabilidad.getProveedor() == null) {
			faltantes.add("proveedor");
		}
		if (trazabilidad.getModelo() == null || trazabilidad.getModelo().isBlank()) {
			faltantes.add("modelo");
		}
		if (trazabilidad.getVersionPrompt() == null || trazabilidad.getVersionPrompt().isBlank()) {
			faltantes.add("versionPrompt");
		}
		if (trazabilidad.getVersionEsquema() == null || trazabilidad.getVersionEsquema().isBlank()) {
			faltantes.add("versionEsquema");
		}
		if (trazabilidad.getValidaciones().isEmpty()) {
			faltantes.add("validacion");
		}
		if (!trazabilidad.getCandidatos().isEmpty() && trazabilidad.getAsociacionSeleccionada() == null) {
			faltantes.add("asociacionSeleccionada");
		}
		if (exigeRevisor(trazabilidad, documento) && trazabilidad.getRevisor() == null) {
			faltantes.add("revisor");
		}
		if (trazabilidad.getEventos().isEmpty()) {
			faltantes.add("eventos");
		}
		trazabilidad.setCompleta(faltantes.isEmpty());
	}

	private boolean exigeRevisor(TrazabilidadDocumentoModel trazabilidad, Documento documento) {
		if (documento.getEstado() != EstadoDocumento.APROBADO && documento.getEstado() != EstadoDocumento.RECHAZADO
				&& documento.getEstado() != EstadoDocumento.CERRADO) {
			return false;
		}
		return trazabilidad.getValidaciones().stream().noneMatch(EjecucionValidacionModel::isAutoaprobado);
	}

	private PoliticaRetencion buscarPolitica(String tenantId, String politicaId) {
		return politicaRetencionRepository.findById(politicaId)
				.filter(politica -> politica.getTenant().getId().equals(tenantId))
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD_POLITICA, politicaId));
	}

	private void aplicarDatos(PoliticaRetencion politica, PoliticaRetencionReqModel datos) {
		politica.setDescripcion(datos.getDescripcion());
		politica.setDuracionDias(datos.getDuracionDias());
		politica.setAccion(datos.getAccion());
		politica.setPermiteRetencionLegal(datos.isPermiteRetencionLegal());
		politica.setActiva(datos.isActiva());
	}

	private Map<String, Object> instantanea(PoliticaRetencion politica) {
		Map<String, Object> valores = new LinkedHashMap<>();
		valores.put("duracionDias", politica.getDuracionDias());
		valores.put("accion", String.valueOf(politica.getAccion()));
		valores.put("permiteRetencionLegal", politica.isPermiteRetencionLegal());
		valores.put("activa", politica.isActiva());
		return valores;
	}
}
