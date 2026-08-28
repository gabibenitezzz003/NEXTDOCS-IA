package com.nextdocs.ai.servicios;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.config.PropiedadesProveedorIa;
import com.nextdocs.ai.entidades.ArchivoDocumento;
import com.nextdocs.ai.entidades.CampoPlantilla;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.EjecucionExtraccion;
import com.nextdocs.ai.entidades.EjecucionValidacion;
import com.nextdocs.ai.entidades.ValorExtraido;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.EstadoEjecucion;
import com.nextdocs.ai.enumeraciones.ResultadoAsociacion;
import com.nextdocs.ai.enumeraciones.ResultadoValidacion;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.enumeraciones.TipoExcepcion;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ProveedorNoDisponibleException;
import com.nextdocs.ai.interfaces.ProveedorDocumentalIaInt;
import com.nextdocs.ai.modelos.CampoEsquemaModel;
import com.nextdocs.ai.modelos.ResultadoAsociacionModel;
import com.nextdocs.ai.modelos.ResultadoExtraccionModel;
import com.nextdocs.ai.modelos.SolicitudExtraccionModel;
import com.nextdocs.ai.modelos.ValorCanonicoModel;
import com.nextdocs.ai.repositorios.ArchivoDocumentoRepository;
import com.nextdocs.ai.repositorios.CampoPlantillaRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.EjecucionExtraccionRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;
import com.nextdocs.ai.servicios.proveedores.RuteadorProveedorService;
import com.nextdocs.ai.servicios.proveedores.NormalizadorValor;
import com.nextdocs.ai.utiles.ContextoCorrelacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractorDocumentalService {

	public static final String ENTIDAD = "EjecucionExtraccion";

	public static final String ENTIDAD_VALIDACION = "EjecucionValidacion";

	public static final String CODIGO_CUOTA = "CUOTA_PROVEEDOR";

	private static final Logger log = LoggerFactory.getLogger(ExtractorDocumentalService.class);

	private final DocumentoRepository documentoRepository;

	private final ArchivoDocumentoRepository archivoDocumentoRepository;

	private final CampoPlantillaRepository campoPlantillaRepository;

	private final EjecucionExtraccionRepository ejecucionExtraccionRepository;

	private final ValorExtraidoRepository valorExtraidoRepository;

	private final RuteadorProveedorService ruteadorProveedorService;

	private final AlmacenamientoService almacenamientoService;

	private final ValidacionDocumentalService validacionDocumentalService;

	private final AsociacionService asociacionService;

	private final SegmentacionDocumentalService segmentacionDocumentalService;

	private final EstadoDocumentalService estadoDocumentalService;

	private final ExcepcionDocumentalService excepcionDocumentalService;

	private final ColaExtraccionService colaExtraccionService;

	private final AuditoriaService auditoriaService;

	private final EventoSalidaService eventoSalidaService;

	private final PropiedadesProveedorIa propiedades;

	public ExtractorDocumentalService(DocumentoRepository documentoRepository,
			ArchivoDocumentoRepository archivoDocumentoRepository, CampoPlantillaRepository campoPlantillaRepository,
			EjecucionExtraccionRepository ejecucionExtraccionRepository,
			ValorExtraidoRepository valorExtraidoRepository, RuteadorProveedorService ruteadorProveedorService,
			AlmacenamientoService almacenamientoService, ValidacionDocumentalService validacionDocumentalService,
			AsociacionService asociacionService, SegmentacionDocumentalService segmentacionDocumentalService,
			EstadoDocumentalService estadoDocumentalService, ExcepcionDocumentalService excepcionDocumentalService,
			ColaExtraccionService colaExtraccionService, AuditoriaService auditoriaService,
			EventoSalidaService eventoSalidaService, PropiedadesProveedorIa propiedades) {
		this.documentoRepository = documentoRepository;
		this.archivoDocumentoRepository = archivoDocumentoRepository;
		this.campoPlantillaRepository = campoPlantillaRepository;
		this.ejecucionExtraccionRepository = ejecucionExtraccionRepository;
		this.valorExtraidoRepository = valorExtraidoRepository;
		this.ruteadorProveedorService = ruteadorProveedorService;
		this.almacenamientoService = almacenamientoService;
		this.validacionDocumentalService = validacionDocumentalService;
		this.asociacionService = asociacionService;
		this.segmentacionDocumentalService = segmentacionDocumentalService;
		this.estadoDocumentalService = estadoDocumentalService;
		this.excepcionDocumentalService = excepcionDocumentalService;
		this.colaExtraccionService = colaExtraccionService;
		this.auditoriaService = auditoriaService;
		this.eventoSalidaService = eventoSalidaService;
		this.propiedades = propiedades;
	}

	@Transactional
	public void procesar(String documentoId) {
		Documento documento = documentoRepository.findById(documentoId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(IngestaDocumentalService.ENTIDAD, documentoId));
		ContextoCorrelacion.establecer(documento.getCorrelacionId());
		if (!estadoDocumentalService.puedeTransicionar(documento, EstadoDocumento.PROCESANDO)) {
			log.warn("El documento {} en estado {} no admite procesamiento", documentoId, documento.getEstado());
			return;
		}
		if (segmentar(documento)) {
			return;
		}
		estadoDocumentalService.transicionar(documento, EstadoDocumento.PROCESANDO);

		EjecucionExtraccion ejecucion = iniciarEjecucion(documento);
		try {
			ResultadoExtraccionModel resultado = ejecutarConProveedor(documento, ejecucion);
			persistirResultado(documento, ejecucion, resultado);
			estadoDocumentalService.transicionar(documento, EstadoDocumento.EXTRAIDO);
			excepcionDocumentalService.resolverAutomaticamente(documento.getTenant().getId(), documento,
					TipoExcepcion.CUOTA_PROVEEDOR, CODIGO_CUOTA);
			validarYResolver(documento, ejecucion);
		} catch (ProveedorNoDisponibleException e) {
			registrarFallo(documento, ejecucion, CODIGO_CUOTA, e.getMessage());
			gestionarReintento(documento, e);
		} catch (Exception e) {
			log.error("Fallo la extraccion del documento {}", documentoId, e);
			registrarFallo(documento, ejecucion, "EXTRACCION_FALLIDA", e.getMessage());
			excepcionDocumentalService.abrir(documento.getTenant(), documento, TipoExcepcion.TECNICA,
					SeveridadHallazgo.REQUIERE_REVISION, "EXTRACCION_FALLIDA", e.getMessage());
			estadoDocumentalService.transicionar(documento, EstadoDocumento.OBSERVADO);
		}
	}

	private boolean segmentar(Documento documento) {
		if (!segmentacionDocumentalService.corresponde(documento)) {
			return false;
		}
		try {
			return segmentacionDocumentalService.segmentar(documento) > 0;
		} catch (Exception e) {
			log.error("Fallo la segmentacion del documento {}, se procesa completo", documento.getId(), e);
			excepcionDocumentalService.abrir(documento.getTenant(), documento, TipoExcepcion.TECNICA,
					SeveridadHallazgo.ADVERTENCIA, "SEGMENTACION_FALLIDA", e.getMessage());
			return false;
		}
	}

	private EjecucionExtraccion iniciarEjecucion(Documento documento) {
		EjecucionExtraccion ejecucion = new EjecucionExtraccion();
		ejecucion.setTenant(documento.getTenant());
		ejecucion.setDocumento(documento);
		ejecucion.setVersionPlantilla(documento.getVersionPlantilla());
		ejecucion.setEstado(EstadoEjecucion.EJECUTANDO);
		ejecucion.setIntento(documento.getReintentosExtraccion() + 1);
		ejecucion.setCorrelacionId(ContextoCorrelacion.obtenerOGenerar());
		ejecucion.setInicio(Instant.now());
		ejecucion.setAlta(Instant.now());
		ejecucion.setProveedor(ruteadorProveedorService.resolverPrincipal(documento.getTenant().getId()).tipo());
		return ejecucionExtraccionRepository.save(ejecucion);
	}

	private ResultadoExtraccionModel ejecutarConProveedor(Documento documento, EjecucionExtraccion ejecucion) {
		ProveedorDocumentalIaInt proveedor = ruteadorProveedorService
				.resolverPrincipal(documento.getTenant().getId());
		SolicitudExtraccionModel solicitud = construirSolicitud(documento);
		try {
			return proveedor.extraer(solicitud);
		} catch (ProveedorNoDisponibleException e) {
			if (!e.esReintentable()) {
				throw e;
			}
			return ruteadorProveedorService.resolverRespaldo(documento.getTenant().getId(), proveedor.tipo())
					.map(respaldo -> {
						log.warn("Usando proveedor de respaldo {} para el documento {}", respaldo.tipo(),
								documento.getId());
						ejecucion.setProveedor(respaldo.tipo());
						return respaldo.extraer(solicitud);
					})
					.orElseThrow(() -> e);
		}
	}

	private SolicitudExtraccionModel construirSolicitud(Documento documento) {
		List<ArchivoDocumento> archivos = archivoDocumentoRepository.listarOriginales(documento.getId());
		if (archivos.isEmpty()) {
			throw new EntidadNoEncontradaException("El documento " + documento.getId() + " no tiene archivo original");
		}
		ArchivoDocumento archivo = archivos.get(0);
		VersionPlantilla version = documento.getVersionPlantilla();
		SolicitudExtraccionModel solicitud = new SolicitudExtraccionModel();
		solicitud.setTenantId(documento.getTenant().getId());
		solicitud.setDocumentoId(documento.getId());
		solicitud.setNombreArchivo(archivo.getNombreArchivo());
		solicitud.setTipoMime(archivo.getTipoMime());
		solicitud.setContenido(almacenamientoService.leerDocumento(archivo.getClaveObjeto()));
		solicitud.setPaginas(archivo.getPaginas());
		solicitud.setCorrelacionId(documento.getCorrelacionId());
		if (documento.getPlantilla() != null) {
			solicitud.setCodigoPlantilla(documento.getPlantilla().getCodigo());
		}
		if (version != null) {
			solicitud.setInstruccionExtraccion(version.getInstruccionExtraccion());
			solicitud.setVersionPrompt(version.getVersionPrompt());
			solicitud.setVersionEsquema(version.getVersionEsquema());
			solicitud.setCampos(construirEsquema(version));
		}
		return solicitud;
	}

	private List<CampoEsquemaModel> construirEsquema(VersionPlantilla version) {
		List<CampoEsquemaModel> esquema = new ArrayList<>();
		for (CampoPlantilla campo : campoPlantillaRepository.listarPorVersion(version.getId())) {
			if (!campo.isExtraer()) {
				continue;
			}
			CampoEsquemaModel modelo = new CampoEsquemaModel();
			modelo.setClave(campo.getClave());
			modelo.setEtiqueta(campo.getEtiqueta());
			modelo.setTipoDato(campo.getTipoDato());
			modelo.setDescripcion(campo.getDescripcion());
			modelo.setAlias(campo.getAlias());
			modelo.setRequerido(campo.isRequerido());
			modelo.setFormatoFecha(campo.getFormatoFecha());
			modelo.setExpresionRegular(campo.getExpresionRegular());
			modelo.setUmbralConfianza(campo.getUmbralConfianza());
			esquema.add(modelo);
		}
		return esquema;
	}

	private void persistirResultado(Documento documento, EjecucionExtraccion ejecucion,
			ResultadoExtraccionModel resultado) {
		ejecucion.setProveedor(resultado.getProveedor());
		ejecucion.setModelo(resultado.getModelo());
		ejecucion.setVersionPrompt(resultado.getVersionPrompt());
		ejecucion.setVersionEsquema(resultado.getVersionEsquema());
		ejecucion.setTokensEntrada(resultado.getTokensEntrada());
		ejecucion.setTokensSalida(resultado.getTokensSalida());
		ejecucion.setPaginasProcesadas(resultado.getPaginasProcesadas());
		ejecucion.setCosto(resultado.getCosto());
		ejecucion.setMonedaCosto(resultado.getMonedaCosto());
		ejecucion.setDuracionMilisegundos(resultado.getDuracionMilisegundos());
		ejecucion.setEstado(EstadoEjecucion.COMPLETADA);
		ejecucion.setFin(Instant.now());
		ejecucionExtraccionRepository.save(ejecucion);

		List<ValorExtraido> valores = new ArrayList<>();
		for (ValorCanonicoModel canonico : resultado.getValores()) {
			ValorExtraido valor = new ValorExtraido();
			valor.setTenant(documento.getTenant());
			valor.setDocumento(documento);
			valor.setEjecucion(ejecucion);
			valor.setClaveCampo(canonico.getClaveCampo());
			valor.setValorCrudo(canonico.getValorCrudo());
			valor.setValorNormalizado(canonico.getValorNormalizado());
			valor.setPresencia(canonico.getPresencia());
			valor.setConfianzaProveedor(canonico.getConfianzaProveedor());
			valor.setConfianza(calibrar(documento.getVersionPlantilla(), canonico));
			valor.setEvidenciaPagina(canonico.getEvidenciaPagina());
			valor.setEvidenciaRecuadro(canonico.getEvidenciaRecuadro());
			valor.setAlta(Instant.now());
			valores.add(valor);
		}
		valorExtraidoRepository.saveAll(valores);
		auditoriaService.registrarConDetalle(documento.getTenant().getId(), AccionAuditoria.EXTRACCION_EJECUTADA,
				ENTIDAD, ejecucion.getId(), Map.of("proveedor", resultado.getProveedor(), "modelo",
						resultado.getModelo() == null ? "" : resultado.getModelo(), "campos", valores.size()));
	}

	private void validarYResolver(Documento documento, EjecucionExtraccion ejecucion) {
		EjecucionValidacion validacion = validacionDocumentalService.validar(documento, ejecucion);
		auditoriaService.registrar(documento.getTenant().getId(), AccionAuditoria.VALIDACION_EJECUTADA,
				ENTIDAD_VALIDACION, validacion.getId());
		if (validacion.getResultado() == ResultadoValidacion.RECHAZADO) {
			estadoDocumentalService.transicionar(documento, EstadoDocumento.OBSERVADO);
			excepcionDocumentalService.abrir(documento.getTenant(), documento, TipoExcepcion.VALIDACION,
					SeveridadHallazgo.BLOQUEANTE, "VALIDACION_BLOQUEANTE", validacion.getMotivoResultado());
			return;
		}
		estadoDocumentalService.transicionar(documento, EstadoDocumento.VALIDADO);

		ResultadoAsociacionModel asociacion = asociar(documento, ejecucion);

		if (validacion.getResultado() == ResultadoValidacion.OBSERVADO) {
			estadoDocumentalService.transicionar(documento, EstadoDocumento.OBSERVADO);
			excepcionDocumentalService.abrir(documento.getTenant(), documento, TipoExcepcion.CALIDAD_LECTURA,
					SeveridadHallazgo.REQUIERE_REVISION, "REVISION_HUMANA", validacion.getMotivoResultado());
			return;
		}
		if (asociacion.requiereRevisionHumana() || asociacion.huboFallo()) {
			estadoDocumentalService.transicionar(documento, EstadoDocumento.OBSERVADO);
			return;
		}
		if (validacion.isAutoaprobado()) {
			estadoDocumentalService.transicionar(documento, EstadoDocumento.APROBADO);
		}
	}

	private ResultadoAsociacionModel asociar(Documento documento, EjecucionExtraccion ejecucion) {
		try {
			return asociacionService.asociar(documento, ejecucion.getId());
		} catch (Exception e) {
			log.error("La asociacion del documento {} fallo de forma inesperada", documento.getId(), e);
			ResultadoAsociacionModel degradado = new ResultadoAsociacionModel();
			degradado.setResultado(ResultadoAsociacion.CONECTOR_FALLIDO);
			degradado.setMotivo(e.getMessage());
			degradado.getConectoresFallidos().add("desconocido");
			return degradado;
		}
	}

	private void registrarFallo(Documento documento, EjecucionExtraccion ejecucion, String codigo, String mensaje) {
		ejecucion.setEstado(EstadoEjecucion.FALLIDA);
		ejecucion.setCodigoError(codigo);
		ejecucion.setMensajeError(mensaje);
		ejecucion.setFin(Instant.now());
		ejecucionExtraccionRepository.save(ejecucion);
		eventoSalidaService.publicar(documento.getTenant().getId(), TipoEventoCanonico.EXTRACCION_FALLIDA, ENTIDAD,
				ejecucion.getId(), Map.of("documentoId", documento.getId(), "codigo", codigo));
	}

	private void gestionarReintento(Documento documento, ProveedorNoDisponibleException e) {
		int reintentos = documento.getReintentosExtraccion() + 1;
		documento.setReintentosExtraccion(reintentos);
		documento.setEstado(EstadoDocumento.RECIBIDO);
		documentoRepository.save(documento);
		if (!e.esReintentable() || reintentos >= propiedades.getIntentosMaximos()) {
			excepcionDocumentalService.abrir(documento.getTenant(), documento, TipoExcepcion.CUOTA_PROVEEDOR,
					SeveridadHallazgo.BLOQUEANTE, CODIGO_CUOTA,
					"Se agotaron los reintentos contra el proveedor de IA: " + e.getMessage());
			return;
		}
		colaExtraccionService.encolarReintento(documento.getId(), esperaDe(reintentos));
	}

	private BigDecimal calibrar(VersionPlantilla version, ValorCanonicoModel canonico) {
		BigDecimal cruda = canonico.getConfianzaProveedor() != null ? canonico.getConfianzaProveedor()
				: canonico.getConfianza();
		if (version == null || version.getFactorCalibracionConfianza() == null || cruda == null) {
			return canonico.getConfianza();
		}
		return NormalizadorValor.calibrarConfianza(cruda.multiply(version.getFactorCalibracionConfianza()));
	}

	private long esperaDe(int reintento) {
		double espera = propiedades.getEsperaInicialMilisegundos()
				* Math.pow(propiedades.getMultiplicadorEspera(), reintento - 1.0);
		return (long) Math.min(espera, propiedades.getEsperaMaximaMilisegundos());
	}
}
