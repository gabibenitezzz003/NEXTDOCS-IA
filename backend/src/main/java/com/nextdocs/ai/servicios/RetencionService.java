package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.nextdocs.ai.convertidores.DocumentoConverter;
import com.nextdocs.ai.entidades.ArchivoDocumento;
import com.nextdocs.ai.entidades.CampoPlantilla;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.PoliticaRetencion;
import com.nextdocs.ai.entidades.ValorExtraido;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.AccionRetencion;
import com.nextdocs.ai.enumeraciones.ResultadoRetencion;
import com.nextdocs.ai.enumeraciones.SensibilidadCampo;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.ResultadoRetencionModel;
import com.nextdocs.ai.modelos.ResumenRetencionModel;
import com.nextdocs.ai.repositorios.ArchivoDocumentoRepository;
import com.nextdocs.ai.repositorios.CampoPlantillaRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.PoliticaRetencionRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetencionService {

	public static final String ENTIDAD = "Documento";

	public static final String VALOR_ANONIMIZADO = "[ANONIMIZADO]";

	public static final int DOCUMENTOS_MAXIMOS_POR_CICLO = 500;

	private static final Set<SensibilidadCampo> SENSIBILIDADES_ANONIMIZABLES = Set.of(SensibilidadCampo.CONFIDENCIAL,
			SensibilidadCampo.PERSONAL);

	private static final Logger log = LoggerFactory.getLogger(RetencionService.class);

	private final DocumentoRepository documentoRepository;

	private final PoliticaRetencionRepository politicaRetencionRepository;

	private final ArchivoDocumentoRepository archivoDocumentoRepository;

	private final ValorExtraidoRepository valorExtraidoRepository;

	private final CampoPlantillaRepository campoPlantillaRepository;

	private final AlmacenamientoService almacenamientoService;

	private final AuditoriaService auditoriaService;

	private final DocumentoConverter documentoConverter;

	public RetencionService(DocumentoRepository documentoRepository,
			PoliticaRetencionRepository politicaRetencionRepository,
			ArchivoDocumentoRepository archivoDocumentoRepository, ValorExtraidoRepository valorExtraidoRepository,
			CampoPlantillaRepository campoPlantillaRepository, AlmacenamientoService almacenamientoService,
			AuditoriaService auditoriaService, DocumentoConverter documentoConverter) {
		this.documentoRepository = documentoRepository;
		this.politicaRetencionRepository = politicaRetencionRepository;
		this.archivoDocumentoRepository = archivoDocumentoRepository;
		this.valorExtraidoRepository = valorExtraidoRepository;
		this.campoPlantillaRepository = campoPlantillaRepository;
		this.almacenamientoService = almacenamientoService;
		this.auditoriaService = auditoriaService;
		this.documentoConverter = documentoConverter;
	}

	@Transactional
	public Instant programar(Documento documento) {
		if (documento.getPlantilla() == null || documento.getCerrado() == null) {
			return null;
		}
		Optional<PoliticaRetencion> politica = politicaRetencionRepository
				.buscarPorClase(documento.getTenant().getId(), documento.getPlantilla().getCodigo());
		if (politica.isEmpty()) {
			return null;
		}
		Instant vencimiento = documento.getCerrado().plus(politica.get().getDuracionDias(), ChronoUnit.DAYS);
		documento.setRetenerHasta(vencimiento);
		log.info("El documento {} retiene hasta {} por la politica {}", documento.getId(), vencimiento,
				politica.get().getClase());
		return vencimiento;
	}

	@Transactional
	public ResumenRetencionModel ejecutarCiclo(int limite) {
		long inicio = System.currentTimeMillis();
		ResumenRetencionModel resumen = new ResumenRetencionModel();
		resumen.setEjecutado(Instant.now());
		int tope = Math.min(Math.max(limite, 1), DOCUMENTOS_MAXIMOS_POR_CICLO);
		Page<Documento> vencidos = documentoRepository.listarVencidosPorRetencion(Instant.now(),
				PageRequest.of(0, tope));
		Map<String, ResumenRetencionModel> porTenant = new LinkedHashMap<>();
		for (Documento documento : vencidos.getContent()) {
			ResultadoRetencionModel resultado = evaluar(documento, false);
			acumular(resumen, resultado);
			acumular(porTenant.computeIfAbsent(documento.getTenant().getId(), id -> new ResumenRetencionModel()),
					resultado);
		}
		resumen.setDuracionMilisegundos(System.currentTimeMillis() - inicio);
		porTenant.forEach(this::auditarCiclo);
		if (resumen.getEvaluados() > 0) {
			log.info("Ciclo de retencion: {} evaluados, {} conservados, {} anonimizados, {} eliminados, "
					+ "{} retenidos por orden legal", resumen.getEvaluados(), resumen.getConservados(),
					resumen.getAnonimizados(), resumen.getEliminados(), resumen.getRetenidosPorRetencionLegal());
		}
		return resumen;
	}

	@Transactional
	public ResultadoRetencionModel aplicar(String tenantId, String documentoId) {
		Documento documento = documentoRepository.buscarPorIdYTenant(documentoId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, documentoId));
		return evaluar(documento, true);
	}

	@Transactional(readOnly = true)
	public Page<DocumentoModel> listarVencidos(String tenantId, Pageable paginado) {
		return documentoRepository.listarVencidosPorTenant(tenantId, Instant.now(), paginado)
				.map(documentoConverter::aModelo);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> inventario(String tenantId) {
		Map<String, Object> inventario = new LinkedHashMap<>();
		inventario.put("vencidosPendientes",
				documentoRepository.listarVencidosPorTenant(tenantId, Instant.now(), PageRequest.of(0, 1))
						.getTotalElements());
		inventario.put("conRetencionLegal", documentoRepository.contarConRetencionLegal(tenantId));
		inventario.put("cerradosSinPolitica", documentoRepository.contarCerradosSinPolitica(tenantId));
		inventario.put("politicasActivas", politicaRetencionRepository.listarPorTenant(tenantId).stream()
				.filter(PoliticaRetencion::isActiva).count());
		return inventario;
	}

	private ResultadoRetencionModel evaluar(Documento documento, boolean auditarOmision) {
		ResultadoRetencionModel resultado = new ResultadoRetencionModel();
		resultado.setDocumentoId(documento.getId());
		resultado.setNombreDocumento(documento.getNombre());
		resultado.setRetenerHasta(documento.getRetenerHasta());
		resultado.setHashContenido(documento.getHashContenido());
		if (documento.getRetencionAplicada() != null) {
			return omitir(resultado, ResultadoRetencion.OMITIDA_YA_APLICADA,
					"La retencion ya se aplico el " + documento.getRetencionAplicada(), documento, auditarOmision);
		}
		if (documento.getRetenerHasta() == null || documento.getRetenerHasta().isAfter(Instant.now())) {
			return omitir(resultado, ResultadoRetencion.OMITIDA_NO_VENCIDA,
					"El documento todavia no alcanzo su fecha de retencion", documento, auditarOmision);
		}
		if (documento.isRetencionLegal()) {
			return omitir(resultado, ResultadoRetencion.OMITIDA_POR_RETENCION_LEGAL,
					"El documento esta bajo retencion legal y no puede tratarse", documento, auditarOmision);
		}
		Optional<PoliticaRetencion> politica = documento.getPlantilla() == null ? Optional.empty()
				: politicaRetencionRepository.buscarPorClase(documento.getTenant().getId(),
						documento.getPlantilla().getCodigo());
		if (politica.isEmpty()) {
			return omitir(resultado, ResultadoRetencion.OMITIDA_SIN_POLITICA,
					"No hay una politica de retencion activa para la clase del documento", documento,
					auditarOmision);
		}
		return ejecutarAccion(documento, politica.get(), resultado);
	}

	private ResultadoRetencionModel ejecutarAccion(Documento documento, PoliticaRetencion politica,
			ResultadoRetencionModel resultado) {
		resultado.setClasePolitica(politica.getClase());
		resultado.setAccion(politica.getAccion());
		resultado.setResultado(ResultadoRetencion.APLICADA);
		List<Map<String, Object>> archivos = new ArrayList<>();
		if (politica.getAccion() == AccionRetencion.ANONIMIZAR
				|| politica.getAccion() == AccionRetencion.ELIMINAR) {
			archivos = eliminarArchivos(documento, resultado);
		}
		if (politica.getAccion() == AccionRetencion.ANONIMIZAR) {
			anonimizarValores(documento, resultado);
		}
		if (politica.getAccion() == AccionRetencion.ELIMINAR) {
			documento.setBaja(Instant.now());
		}
		Instant aplicada = Instant.now();
		documento.setRetencionAplicada(aplicada);
		documento.setAccionRetencionAplicada(politica.getAccion());
		documentoRepository.save(documento);
		resultado.setAplicada(aplicada);
		auditarAplicacion(documento, politica, resultado, archivos);
		log.info("Retencion {} aplicada al documento {} por la politica {}", politica.getAccion(),
				documento.getId(), politica.getClase());
		return resultado;
	}

	private List<Map<String, Object>> eliminarArchivos(Documento documento, ResultadoRetencionModel resultado) {
		List<Map<String, Object>> evidencia = new ArrayList<>();
		for (ArchivoDocumento archivo : archivoDocumentoRepository.listarPorDocumento(documento.getId())) {
			Map<String, Object> detalle = new LinkedHashMap<>();
			detalle.put("nombreArchivo", String.valueOf(archivo.getNombreArchivo()));
			detalle.put("checksum", String.valueOf(archivo.getChecksum()));
			detalle.put("tamano", archivo.getTamano());
			try {
				almacenamientoService.eliminarDocumento(archivo.getClaveObjeto());
				detalle.put("eliminado", true);
				resultado.setArchivosEliminados(resultado.getArchivosEliminados() + 1);
			} catch (Exception e) {
				detalle.put("eliminado", false);
				detalle.put("error", String.valueOf(e.getMessage()));
				log.error("No se pudo eliminar el objeto {} del documento {}", archivo.getClaveObjeto(),
						documento.getId(), e);
			}
			archivo.setBaja(Instant.now());
			archivoDocumentoRepository.save(archivo);
			evidencia.add(detalle);
		}
		return evidencia;
	}

	private void anonimizarValores(Documento documento, ResultadoRetencionModel resultado) {
		Set<String> sensibles = clavesSensibles(documento);
		if (sensibles.isEmpty()) {
			return;
		}
		for (ValorExtraido valor : valorExtraidoRepository.listarPorDocumento(documento.getId())) {
			if (!sensibles.contains(valor.getClaveCampo())) {
				continue;
			}
			valor.setValorCrudo(VALOR_ANONIMIZADO);
			valor.setValorNormalizado(VALOR_ANONIMIZADO);
			valor.setValorAnterior(null);
			valor.setEvidenciaRecuadro(null);
			valor.setAnonimizado(true);
			valorExtraidoRepository.save(valor);
			resultado.setValoresAnonimizados(resultado.getValoresAnonimizados() + 1);
			if (!resultado.getClavesAnonimizadas().contains(valor.getClaveCampo())) {
				resultado.getClavesAnonimizadas().add(valor.getClaveCampo());
			}
		}
	}

	private Set<String> clavesSensibles(Documento documento) {
		Set<String> claves = new HashSet<>();
		if (documento.getVersionPlantilla() == null) {
			return claves;
		}
		for (CampoPlantilla campo : campoPlantillaRepository
				.listarPorVersion(documento.getVersionPlantilla().getId())) {
			if (campo.getSensibilidad() != null && SENSIBILIDADES_ANONIMIZABLES.contains(campo.getSensibilidad())) {
				claves.add(campo.getClave());
			}
		}
		return claves;
	}

	private ResultadoRetencionModel omitir(ResultadoRetencionModel resultado, ResultadoRetencion motivo,
			String descripcion, Documento documento, boolean auditar) {
		resultado.setResultado(motivo);
		resultado.setMotivo(descripcion);
		if (auditar) {
			Map<String, Object> detalle = new LinkedHashMap<>();
			detalle.put("resultado", motivo.name());
			detalle.put("motivo", descripcion);
			detalle.put("retenerHasta", String.valueOf(documento.getRetenerHasta()));
			detalle.put("retencionLegal", documento.isRetencionLegal());
			auditoriaService.registrarConDetalle(documento.getTenant().getId(),
					AccionAuditoria.RETENCION_APLICADA, ENTIDAD, documento.getId(), detalle);
		}
		return resultado;
	}

	private void auditarAplicacion(Documento documento, PoliticaRetencion politica,
			ResultadoRetencionModel resultado, List<Map<String, Object>> archivos) {
		Map<String, Object> detalle = new LinkedHashMap<>();
		detalle.put("resultado", ResultadoRetencion.APLICADA.name());
		detalle.put("politicaId", politica.getId());
		detalle.put("clase", politica.getClase());
		detalle.put("accion", politica.getAccion().name());
		detalle.put("duracionDias", politica.getDuracionDias());
		detalle.put("cerrado", String.valueOf(documento.getCerrado()));
		detalle.put("retenerHasta", String.valueOf(documento.getRetenerHasta()));
		detalle.put("hashContenido", String.valueOf(documento.getHashContenido()));
		detalle.put("archivosEliminados", resultado.getArchivosEliminados());
		detalle.put("valoresAnonimizados", resultado.getValoresAnonimizados());
		detalle.put("clavesAnonimizadas", resultado.getClavesAnonimizadas());
		detalle.put("archivos", archivos);
		auditoriaService.registrarConDetalle(documento.getTenant().getId(), AccionAuditoria.RETENCION_APLICADA,
				ENTIDAD, documento.getId(), detalle);
	}

	private void auditarCiclo(String tenantId, ResumenRetencionModel resumen) {
		Map<String, Object> detalle = new HashMap<>();
		detalle.put("evaluados", resumen.getEvaluados());
		detalle.put("conservados", resumen.getConservados());
		detalle.put("anonimizados", resumen.getAnonimizados());
		detalle.put("eliminados", resumen.getEliminados());
		detalle.put("retenidosPorRetencionLegal", resumen.getRetenidosPorRetencionLegal());
		detalle.put("omitidosSinPolitica", resumen.getOmitidosSinPolitica());
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.RETENCION_CICLO_EJECUTADO, "Tenant",
				tenantId, detalle);
	}

	private void acumular(ResumenRetencionModel resumen, ResultadoRetencionModel resultado) {
		resumen.setEvaluados(resumen.getEvaluados() + 1);
		resumen.getDetalle().add(resultado);
		if (resultado.getResultado() == ResultadoRetencion.OMITIDA_POR_RETENCION_LEGAL) {
			resumen.setRetenidosPorRetencionLegal(resumen.getRetenidosPorRetencionLegal() + 1);
			return;
		}
		if (resultado.getResultado() == ResultadoRetencion.OMITIDA_SIN_POLITICA) {
			resumen.setOmitidosSinPolitica(resumen.getOmitidosSinPolitica() + 1);
			return;
		}
		if (resultado.getResultado() != ResultadoRetencion.APLICADA) {
			return;
		}
		if (resultado.getAccion() == AccionRetencion.CONSERVAR) {
			resumen.setConservados(resumen.getConservados() + 1);
		}
		if (resultado.getAccion() == AccionRetencion.ANONIMIZAR) {
			resumen.setAnonimizados(resumen.getAnonimizados() + 1);
		}
		if (resultado.getAccion() == AccionRetencion.ELIMINAR) {
			resumen.setEliminados(resumen.getEliminados() + 1);
		}
	}
}
