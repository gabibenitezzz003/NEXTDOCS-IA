package com.nextdocs.ai.servicios;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.convertidores.DocumentoConverter;
import com.nextdocs.ai.entidades.CandidatoAsociacion;
import com.nextdocs.ai.entidades.ConfiguracionConector;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.ReferenciaExterna;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.entidades.ValorExtraido;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.ResultadoAsociacion;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.enumeraciones.TipoExcepcion;
import com.nextdocs.ai.exceptions.ConectorNoDisponibleException;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.interfaces.ConectorAsociacionInt;
import com.nextdocs.ai.modelos.CandidatoAsociacionModel;
import com.nextdocs.ai.modelos.ContextoAsociacionModel;
import com.nextdocs.ai.modelos.ResultadoAsociacionModel;
import com.nextdocs.ai.repositorios.CandidatoAsociacionRepository;
import com.nextdocs.ai.repositorios.ConfiguracionConectorRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;
import com.nextdocs.ai.utiles.CircuitoConector;
import com.nextdocs.ai.utiles.ContextoCorrelacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsociacionService {

	public static final String ENTIDAD = "CandidatoAsociacion";

	public static final String CODIGO_CONECTOR_FALLIDO = "CONECTOR_FALLIDO";

	public static final String CODIGO_ASOCIACION_AMBIGUA = "ASOCIACION_AMBIGUA";

	private static final BigDecimal UMBRAL_SELECCION_POR_DEFECTO = new BigDecimal("0.9500");

	private static final BigDecimal UMBRAL_MINIMO_POR_DEFECTO = new BigDecimal("0.5000");

	private static final Logger log = LoggerFactory.getLogger(AsociacionService.class);

	private final Map<String, ConectorAsociacionInt> conectores;

	private final ConfiguracionConectorRepository configuracionConectorRepository;

	private final CandidatoAsociacionRepository candidatoAsociacionRepository;

	private final ValorExtraidoRepository valorExtraidoRepository;

	private final DocumentoRepository documentoRepository;

	private final RegistroCircuitosService registroCircuitosService;

	private final ExcepcionDocumentalService excepcionDocumentalService;

	private final AuditoriaService auditoriaService;

	private final EventoSalidaService eventoSalidaService;

	private final DocumentoConverter documentoConverter;

	public AsociacionService(List<ConectorAsociacionInt> conectoresDisponibles,
			ConfiguracionConectorRepository configuracionConectorRepository,
			CandidatoAsociacionRepository candidatoAsociacionRepository,
			ValorExtraidoRepository valorExtraidoRepository, DocumentoRepository documentoRepository,
			RegistroCircuitosService registroCircuitosService,
			ExcepcionDocumentalService excepcionDocumentalService, AuditoriaService auditoriaService,
			EventoSalidaService eventoSalidaService, DocumentoConverter documentoConverter) {
		this.conectores = new java.util.HashMap<>();
		for (ConectorAsociacionInt conector : conectoresDisponibles) {
			this.conectores.put(conector.codigo(), conector);
		}
		this.configuracionConectorRepository = configuracionConectorRepository;
		this.candidatoAsociacionRepository = candidatoAsociacionRepository;
		this.valorExtraidoRepository = valorExtraidoRepository;
		this.documentoRepository = documentoRepository;
		this.registroCircuitosService = registroCircuitosService;
		this.excepcionDocumentalService = excepcionDocumentalService;
		this.auditoriaService = auditoriaService;
		this.eventoSalidaService = eventoSalidaService;
		this.documentoConverter = documentoConverter;
	}

	@Transactional
	public ResultadoAsociacionModel asociar(Documento documento, String ejecucionId) {
		String tenantId = documento.getTenant().getId();
		ResultadoAsociacionModel resultado = new ResultadoAsociacionModel();

		List<ConfiguracionConector> configuraciones = configuracionConectorRepository.listarActivos(tenantId);
		if (configuraciones.isEmpty()) {
			resultado.setResultado(ResultadoAsociacion.NO_APLICA);
			resultado.setMotivo("El tenant no tiene conectores de asociacion configurados");
			return resultado;
		}

		ContextoAsociacionModel contexto = construirContexto(documento, ejecucionId);
		if (contexto.getValores().isEmpty()) {
			resultado.setResultado(ResultadoAsociacion.NO_APLICA);
			resultado.setMotivo("El documento no tiene valores legibles para asociar");
			return resultado;
		}

		List<CandidatoAsociacion> encontrados = new ArrayList<>();
		for (ConfiguracionConector configuracion : configuraciones) {
			consultarConector(documento, configuracion, contexto, resultado, encontrados);
		}

		candidatoAsociacionRepository.saveAll(encontrados);
		resultado.setCantidadCandidatos(encontrados.size());
		resultado.setCandidatos(documentoConverter.aModelosCandidatos(encontrados));
		resolver(documento, configuraciones, encontrados, resultado);
		return resultado;
	}

	@Transactional
	public ResultadoAsociacionModel seleccionar(String tenantId, String documentoId, String candidatoId,
			Usuario actor, String motivo) {
		CandidatoAsociacion candidato = candidatoAsociacionRepository.buscarPorIdYTenant(candidatoId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, candidatoId));
		if (candidato.getDocumento() == null || !documentoId.equals(candidato.getDocumento().getId())) {
			throw new ValidacionException("El candidato no pertenece al documento indicado");
		}
		Documento documento = candidato.getDocumento();

		for (CandidatoAsociacion otro : candidatoAsociacionRepository.listarPorDocumento(documentoId)) {
			boolean elegido = otro.getId().equals(candidatoId);
			otro.setSeleccionado(elegido);
			otro.setDescartado(!elegido);
			if (elegido) {
				otro.setSeleccionadoPor(actor);
				otro.setMotivoSeleccion(motivo);
			}
			candidatoAsociacionRepository.save(otro);
		}

		aplicarReferencia(documento, candidato);
		documentoRepository.save(documento);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.REVISION_REGISTRADA, ENTIDAD, candidatoId,
				Map.of("documentoId", documentoId, "conector", candidato.getConector(), "objeto",
						candidato.getReferencia() == null ? "" : String.valueOf(candidato.getReferencia().getIdObjeto())));

		ResultadoAsociacionModel resultado = new ResultadoAsociacionModel();
		resultado.setResultado(ResultadoAsociacion.RESUELTA);
		resultado.setCandidatoSeleccionadoId(candidatoId);
		resultado.setMotivo("Seleccion humana");
		resultado.setCandidatos(
				documentoConverter.aModelosCandidatos(candidatoAsociacionRepository.listarPorDocumento(documentoId)));
		resultado.setCantidadCandidatos(resultado.getCandidatos().size());
		return resultado;
	}

	@Transactional(readOnly = true)
	public List<CandidatoAsociacionModel> listar(String tenantId, String documentoId) {
		return documentoConverter.aModelosCandidatos(candidatoAsociacionRepository.listarPorDocumento(documentoId));
	}

	private void consultarConector(Documento documento, ConfiguracionConector configuracion,
			ContextoAsociacionModel contexto, ResultadoAsociacionModel resultado,
			List<CandidatoAsociacion> encontrados) {
		String codigo = configuracion.getCodigo();
		ConectorAsociacionInt conector = conectores.get(codigo);
		if (conector == null) {
			log.warn("No hay implementacion registrada para el conector {}", codigo);
			return;
		}
		resultado.getConectoresConsultados().add(codigo);

		CircuitoConector circuito = registroCircuitosService.obtener(configuracion);
		if (!circuito.permitePasar()) {
			registrarFalloConector(documento, resultado, codigo,
					"El circuito del conector " + codigo + " esta abierto");
			return;
		}

		try {
			for (CandidatoAsociacionModel modelo : conector.buscarCandidatos(contexto)) {
				encontrados.add(construirCandidato(documento, modelo));
			}
			circuito.registrarExito();
		} catch (ConectorNoDisponibleException e) {
			circuito.registrarFallo();
			registrarFalloConector(documento, resultado, codigo, e.getMessage());
		} catch (Exception e) {
			circuito.registrarFallo();
			log.error("Fallo inesperado del conector {}", codigo, e);
			registrarFalloConector(documento, resultado, codigo, e.getMessage());
		}
	}

	private void registrarFalloConector(Documento documento, ResultadoAsociacionModel resultado, String codigo,
			String detalle) {
		resultado.getConectoresFallidos().add(codigo);
		excepcionDocumentalService.abrir(documento.getTenant(), documento, TipoExcepcion.CONECTOR,
				SeveridadHallazgo.REQUIERE_REVISION, CODIGO_CONECTOR_FALLIDO,
				"El conector " + codigo + " no respondio: " + detalle);
		eventoSalidaService.publicar(documento.getTenant().getId(),
				TipoEventoCanonico.ACCION_CONECTOR_FALLIDA, ENTIDAD, documento.getId(),
				Map.of("conector", codigo, "detalle", detalle == null ? "" : detalle));
	}

	private void resolver(Documento documento, List<ConfiguracionConector> configuraciones,
			List<CandidatoAsociacion> encontrados, ResultadoAsociacionModel resultado) {
		BigDecimal umbralSeleccion = umbralSeleccion(configuraciones);
		BigDecimal umbralMinimo = umbralMinimo(configuraciones);

		List<CandidatoAsociacion> viables = new ArrayList<>();
		for (CandidatoAsociacion candidato : encontrados) {
			if (candidato.getPuntaje() != null && candidato.getPuntaje().compareTo(umbralMinimo) >= 0) {
				viables.add(candidato);
			}
		}
		viables.sort(Comparator.comparing(CandidatoAsociacion::getPuntaje).reversed());

		if (viables.isEmpty()) {
			resultado.setResultado(resultado.getConectoresFallidos().isEmpty() ? ResultadoAsociacion.SIN_CANDIDATOS
					: ResultadoAsociacion.CONECTOR_FALLIDO);
			resultado.setMotivo(resultado.getConectoresFallidos().isEmpty()
					? "Ningun conector devolvio candidatos por encima del umbral minimo"
					: "No hay candidatos y ademas fallaron conectores: " + resultado.getConectoresFallidos());
			return;
		}

		if (viables.size() > 1 || !resultado.getConectoresFallidos().isEmpty()) {
			resultado.setResultado(ResultadoAsociacion.AMBIGUA);
			resultado.setMotivo(viables.size() > 1
					? "Hay " + viables.size() + " candidatos compatibles, requiere seleccion humana"
					: "Hay un candidato pero fallaron conectores, la busqueda esta incompleta");
			excepcionDocumentalService.abrir(documento.getTenant(), documento, TipoExcepcion.ASOCIACION,
					SeveridadHallazgo.REQUIERE_REVISION, CODIGO_ASOCIACION_AMBIGUA, resultado.getMotivo());
			return;
		}

		CandidatoAsociacion unico = viables.get(0);
		if (unico.getPuntaje().compareTo(umbralSeleccion) < 0) {
			resultado.setResultado(ResultadoAsociacion.AMBIGUA);
			resultado.setMotivo("El unico candidato tiene puntaje " + unico.getPuntaje()
					+ ", por debajo del umbral de seleccion automatica " + umbralSeleccion);
			excepcionDocumentalService.abrir(documento.getTenant(), documento, TipoExcepcion.ASOCIACION,
					SeveridadHallazgo.REQUIERE_REVISION, CODIGO_ASOCIACION_AMBIGUA, resultado.getMotivo());
			return;
		}

		unico.setSeleccionado(true);
		unico.setMotivoSeleccion("Candidato unico con puntaje " + unico.getPuntaje());
		candidatoAsociacionRepository.save(unico);
		aplicarReferencia(documento, unico);
		documentoRepository.save(documento);

		resultado.setResultado(ResultadoAsociacion.RESUELTA);
		resultado.setCandidatoSeleccionadoId(unico.getId());
		resultado.setMotivo(unico.getMotivoSeleccion());
	}

	private void aplicarReferencia(Documento documento, CandidatoAsociacion candidato) {
		if (candidato.getReferencia() == null) {
			return;
		}
		ReferenciaExterna referencia = new ReferenciaExterna();
		referencia.setOrigen(candidato.getReferencia().getOrigen());
		referencia.setTipoObjeto(candidato.getReferencia().getTipoObjeto());
		referencia.setIdObjeto(candidato.getReferencia().getIdObjeto());
		referencia.setTenantOrigen(candidato.getReferencia().getTenantOrigen());
		documento.setReferenciaSujeto(referencia);
	}

	private ContextoAsociacionModel construirContexto(Documento documento, String ejecucionId) {
		ContextoAsociacionModel contexto = new ContextoAsociacionModel();
		contexto.setTenantId(documento.getTenant().getId());
		contexto.setDocumentoId(documento.getId());
		contexto.setCorrelacionId(ContextoCorrelacion.obtenerOGenerar());
		if (documento.getPlantilla() != null) {
			contexto.setCodigoPlantilla(documento.getPlantilla().getCodigo());
		}
		for (ValorExtraido valor : valorExtraidoRepository.listarPorEjecucion(ejecucionId)) {
			if (valor.getPresencia() != PresenciaCampo.PRESENTE) {
				continue;
			}
			String texto = valor.getValorNormalizado() == null ? valor.getValorCrudo() : valor.getValorNormalizado();
			if (texto != null && !texto.isBlank()) {
				contexto.getValores().put(valor.getClaveCampo(), texto);
			}
		}
		return contexto;
	}

	private CandidatoAsociacion construirCandidato(Documento documento, CandidatoAsociacionModel modelo) {
		CandidatoAsociacion candidato = new CandidatoAsociacion();
		candidato.setTenant(documento.getTenant());
		candidato.setDocumento(documento);
		candidato.setConector(modelo.getConector());
		candidato.setPuntaje(modelo.getPuntaje());
		candidato.setRazones(modelo.getRazones());
		candidato.setDescripcion(modelo.getDescripcion());
		candidato.setAlta(Instant.now());
		ReferenciaExterna referencia = new ReferenciaExterna();
		referencia.setOrigen(modelo.getOrigen());
		referencia.setTipoObjeto(modelo.getTipoObjeto());
		referencia.setIdObjeto(modelo.getIdObjeto());
		candidato.setReferencia(referencia);
		return candidato;
	}

	private BigDecimal umbralSeleccion(List<ConfiguracionConector> configuraciones) {
		return configuraciones.stream().map(ConfiguracionConector::getUmbralSeleccionAutomatica)
				.filter(java.util.Objects::nonNull).findFirst().orElse(UMBRAL_SELECCION_POR_DEFECTO);
	}

	private BigDecimal umbralMinimo(List<ConfiguracionConector> configuraciones) {
		return configuraciones.stream().map(ConfiguracionConector::getUmbralCandidatoMinimo)
				.filter(java.util.Objects::nonNull).findFirst().orElse(UMBRAL_MINIMO_POR_DEFECTO);
	}
}
