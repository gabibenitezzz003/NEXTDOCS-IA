package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.nextdocs.ai.convertidores.CanalCorreoConverter;
import com.nextdocs.ai.entidades.AdjuntoCorreo;
import com.nextdocs.ai.entidades.BuzonCorreo;
import com.nextdocs.ai.entidades.CorrelacionCorreo;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.MensajeCorreoEntrante;
import com.nextdocs.ai.entidades.RemitenteAutorizado;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.OrigenDocumento;
import com.nextdocs.ai.enumeraciones.PlantillaCorreo;
import com.nextdocs.ai.enumeraciones.ResultadoAdjuntoCorreo;
import com.nextdocs.ai.enumeraciones.ResultadoMensajeCorreo;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.enumeraciones.TipoExcepcion;
import com.nextdocs.ai.exceptions.ArchivoRechazadoException;
import com.nextdocs.ai.modelos.CorreoCrudoModel;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.MensajeCorreoModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.repositorios.AdjuntoCorreoRepository;
import com.nextdocs.ai.repositorios.CorrelacionCorreoRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.MensajeCorreoEntranteRepository;
import com.nextdocs.ai.repositorios.RemitenteAutorizadoRepository;
import com.nextdocs.ai.utiles.ArchivoEnMemoria;
import com.nextdocs.ai.utiles.ContextoCorrelacion;
import com.nextdocs.ai.utiles.DireccionCorreo;
import com.nextdocs.ai.utiles.Hash;
import com.nextdocs.ai.utiles.TokenCorrelacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IngestaCorreoService {

	public static final String ENTIDAD = "MensajeCorreoEntrante";

	public static final String CODIGO_SIN_CORRELACION = "CORREO_SIN_CORRELACION";

	private static final Logger log = LoggerFactory.getLogger(IngestaCorreoService.class);

	private final MensajeCorreoEntranteRepository mensajeCorreoEntranteRepository;

	private final AdjuntoCorreoRepository adjuntoCorreoRepository;

	private final CorrelacionCorreoRepository correlacionCorreoRepository;

	private final RemitenteAutorizadoRepository remitenteAutorizadoRepository;

	private final DocumentoRepository documentoRepository;

	private final IngestaDocumentalService ingestaDocumentalService;

	private final ExcepcionDocumentalService excepcionDocumentalService;

	private final CorreoSalienteService correoSalienteService;

	private final AuditoriaService auditoriaService;

	private final EventoSalidaService eventoSalidaService;

	private final CanalCorreoConverter canalCorreoConverter;

	public IngestaCorreoService(MensajeCorreoEntranteRepository mensajeCorreoEntranteRepository,
			AdjuntoCorreoRepository adjuntoCorreoRepository,
			CorrelacionCorreoRepository correlacionCorreoRepository,
			RemitenteAutorizadoRepository remitenteAutorizadoRepository, DocumentoRepository documentoRepository,
			IngestaDocumentalService ingestaDocumentalService,
			ExcepcionDocumentalService excepcionDocumentalService, CorreoSalienteService correoSalienteService,
			AuditoriaService auditoriaService, EventoSalidaService eventoSalidaService,
			CanalCorreoConverter canalCorreoConverter) {
		this.mensajeCorreoEntranteRepository = mensajeCorreoEntranteRepository;
		this.adjuntoCorreoRepository = adjuntoCorreoRepository;
		this.correlacionCorreoRepository = correlacionCorreoRepository;
		this.remitenteAutorizadoRepository = remitenteAutorizadoRepository;
		this.documentoRepository = documentoRepository;
		this.ingestaDocumentalService = ingestaDocumentalService;
		this.excepcionDocumentalService = excepcionDocumentalService;
		this.correoSalienteService = correoSalienteService;
		this.auditoriaService = auditoriaService;
		this.eventoSalidaService = eventoSalidaService;
		this.canalCorreoConverter = canalCorreoConverter;
	}

	public MensajeCorreoModel procesar(BuzonCorreo buzon, CorreoCrudoModel correo) {
		Optional<MensajeCorreoEntrante> yaProcesado = mensajeCorreoEntranteRepository
				.buscarPorIdentificador(buzon.getId(), correo.getIdentificadorMensaje());
		if (yaProcesado.isPresent()) {
			log.debug("El mensaje {} del buzon {} ya estaba procesado", correo.getIdentificadorMensaje(),
					buzon.getDireccion());
			return null;
		}

		ContextoCorrelacion.establecer(java.util.UUID.randomUUID().toString());
		MensajeCorreoEntrante mensaje = registrarRecepcion(buzon, correo);

		String remitente = DireccionCorreo.normalizar(correo.getRemitente());
		if (!estaAutorizado(buzon, remitente)) {
			return cerrarSinIngesta(buzon, mensaje, ResultadoMensajeCorreo.REMITENTE_NO_AUTORIZADO,
					"El remitente " + remitente + " no esta en la lista de autorizados del buzon",
					PlantillaCorreo.AVISO_REMITENTE_NO_AUTORIZADO);
		}

		if (correo.getAdjuntos().isEmpty()) {
			return cerrarSinIngesta(buzon, mensaje, ResultadoMensajeCorreo.SIN_ADJUNTOS,
					"El mensaje no trae adjuntos para ingestar", null);
		}

		String token = detectarToken(buzon, correo);
		mensaje.setTokenDetectado(token);
		CorrelacionCorreo correlacion = resolver(buzon, token);
		String problemaCorrelacion = evaluarCorrelacion(buzon, token, correlacion);
		mensaje.setCorrelacion(correlacion);

		List<AdjuntoCorreo> adjuntos = ingestar(buzon, mensaje, correo, correlacion);
		int ingestados = (int) adjuntos.stream()
				.filter(adjunto -> adjunto.getResultado() == ResultadoAdjuntoCorreo.INGESTADO).count();
		int rechazados = adjuntos.size() - ingestados;

		mensaje.setAdjuntos(adjuntos.size());
		mensaje.setIngestados(ingestados);
		mensaje.setRechazados(rechazados);
		mensaje.setResultado(resultadoDe(problemaCorrelacion, adjuntos.size(), ingestados));
		mensaje.setMotivo(problemaCorrelacion);
		mensajeCorreoEntranteRepository.save(mensaje);

		if (problemaCorrelacion != null) {
			abrirExcepcionesDeAsociacion(buzon, adjuntos, problemaCorrelacion);
		}
		else if (correlacion != null && ingestados > 0) {
			correlacion.setDocumentosRecibidos(correlacion.getDocumentosRecibidos() + ingestados);
			correlacion.setUltimoUso(Instant.now());
			correlacionCorreoRepository.save(correlacion);
		}

		auditar(buzon, mensaje, adjuntos);
		publicar(buzon, mensaje);
		responderSiCorresponde(buzon, mensaje, correlacion, ingestados, rechazados);
		return canalCorreoConverter.aModelo(mensaje, adjuntos);
	}

	private MensajeCorreoEntrante registrarRecepcion(BuzonCorreo buzon, CorreoCrudoModel correo) {
		MensajeCorreoEntrante mensaje = new MensajeCorreoEntrante();
		mensaje.setTenant(buzon.getTenant());
		mensaje.setBuzon(buzon);
		mensaje.setIdentificadorMensaje(correo.getIdentificadorMensaje());
		mensaje.setRemitente(DireccionCorreo.normalizar(correo.getRemitente()));
		mensaje.setDestinatarios(String.join(", ", correo.getDestinatarios()));
		mensaje.setAsunto(recortar(correo.getAsunto(), 400));
		mensaje.setEnviadoEn(correo.getEnviadoEn());
		mensaje.setResultado(ResultadoMensajeCorreo.ERROR);
		mensaje.setMotivo("El procesamiento del mensaje no termino");
		mensaje.setCorrelacionTraza(ContextoCorrelacion.obtenerOGenerar());
		mensaje.setAlta(Instant.now());
		return mensajeCorreoEntranteRepository.save(mensaje);
	}

	private boolean estaAutorizado(BuzonCorreo buzon, String remitente) {
		if (!buzon.isExigirRemitenteAutorizado()) {
			return true;
		}
		if (remitente == null) {
			return false;
		}
		List<RemitenteAutorizado> autorizados = remitenteAutorizadoRepository.listarPorBuzon(buzon.getId());
		return autorizados.stream().anyMatch(autorizado -> DireccionCorreo.coincide(autorizado.getPatron(), remitente));
	}

	private String detectarToken(BuzonCorreo buzon, CorreoCrudoModel correo) {
		List<String> candidatos = new ArrayList<>();
		candidatos.add(correo.getAsunto());
		String direccionBuzon = DireccionCorreo.normalizar(buzon.getDireccion());
		for (String destinatario : correo.getDestinatarios()) {
			if (direccionBuzon != null && direccionBuzon.equals(DireccionCorreo.sinEtiqueta(destinatario))) {
				candidatos.add(DireccionCorreo.etiqueta(destinatario));
			}
		}
		return TokenCorrelacion.detectar(candidatos.toArray(new String[0])).orElse(null);
	}

	private CorrelacionCorreo resolver(BuzonCorreo buzon, String token) {
		if (token == null) {
			return null;
		}
		return correlacionCorreoRepository.buscarPorTokenYTenant(token, buzon.getTenant().getId())
				.filter(candidata -> candidata.getBuzon() != null
						&& candidata.getBuzon().getId().equals(buzon.getId()))
				.filter(candidata -> candidata.getVenceEn() == null
						|| candidata.getVenceEn().isAfter(Instant.now()))
				.orElse(null);
	}

	private String evaluarCorrelacion(BuzonCorreo buzon, String token, CorrelacionCorreo correlacion) {
		if (correlacion != null) {
			return null;
		}
		if (token != null) {
			return "El token " + token + " no corresponde a una solicitud vigente de este buzon: "
					+ "los documentos quedaron sin asociar para revision humana";
		}
		if (buzon.isExigirCorrelacion()) {
			return "El mensaje no trae token de correlacion y el buzon lo exige: "
					+ "los documentos quedaron sin asociar para revision humana";
		}
		return null;
	}

	private List<AdjuntoCorreo> ingestar(BuzonCorreo buzon, MensajeCorreoEntrante mensaje, CorreoCrudoModel correo,
			CorrelacionCorreo correlacion) {
		List<AdjuntoCorreo> resultados = new ArrayList<>();
		int tope = buzon.getMaximoAdjuntosPorMensaje() <= 0 ? correo.getAdjuntos().size()
				: buzon.getMaximoAdjuntosPorMensaje();
		for (int indice = 0; indice < correo.getAdjuntos().size(); indice++) {
			CorreoCrudoModel.AdjuntoCrudoModel crudo = correo.getAdjuntos().get(indice);
			AdjuntoCorreo adjunto = new AdjuntoCorreo();
			adjunto.setTenant(buzon.getTenant());
			adjunto.setMensaje(mensaje);
			adjunto.setNombreArchivo(recortar(crudo.getNombre(), 400));
			adjunto.setTipoMime(recortar(crudo.getTipoMimeDeclarado(), 128));
			adjunto.setTamanoBytes(crudo.getContenido() == null ? 0 : crudo.getContenido().length);
			adjunto.setAlta(Instant.now());

			if (crudo.getContenido() == null) {
				marcarRechazo(adjunto, "TAMANO_EXCEDIDO", "El adjunto supera el tope de lectura del canal");
			}
			else if (indice >= tope) {
				marcarRechazo(adjunto, "DEMASIADOS_ADJUNTOS",
						"El mensaje supera el maximo de " + tope + " adjuntos configurado en el buzon");
			}
			else {
				adjunto.setSha256(Hash.sha256(crudo.getContenido()));
				aplicarIngesta(buzon, mensaje, correlacion, crudo, adjunto, indice);
			}
			resultados.add(adjuntoCorreoRepository.save(adjunto));
		}
		return resultados;
	}

	private void aplicarIngesta(BuzonCorreo buzon, MensajeCorreoEntrante mensaje, CorrelacionCorreo correlacion,
			CorreoCrudoModel.AdjuntoCrudoModel crudo, AdjuntoCorreo adjunto, int indice) {
		try {
			DocumentoModel documento = ingestaDocumentalService.ingresar(buzon.getTenant(), null,
					new ArchivoEnMemoria(crudo.getNombre(), crudo.getTipoMimeDeclarado(), crudo.getContenido()),
					datosDe(buzon, mensaje, correlacion),
					"correo:" + mensaje.getIdentificadorMensaje() + ":" + indice);
			Documento entidad = documentoRepository.findById(documento.getId()).orElse(null);
			adjunto.setDocumento(entidad);
			if (entidad != null && entidad.getEstado() == EstadoDocumento.RECHAZADO) {
				adjunto.setResultado(ResultadoAdjuntoCorreo.EN_CUARENTENA);
				adjunto.setCodigoRechazo(IngestaDocumentalService.CODIGO_ARCHIVO_INFECTADO);
				adjunto.setMotivo(recortar(entidad.getObservacion(), 400));
			}
			else {
				adjunto.setResultado(ResultadoAdjuntoCorreo.INGESTADO);
			}
		}
		catch (ArchivoRechazadoException e) {
			marcarRechazo(adjunto, e.getCodigo(), e.getMessage());
		}
		catch (Exception e) {
			adjunto.setResultado(ResultadoAdjuntoCorreo.ERROR);
			adjunto.setCodigoRechazo("ERROR_INGESTA");
			adjunto.setMotivo(recortar(e.getMessage(), 400));
			log.warn("Fallo la ingesta del adjunto {} del mensaje {}: {}", crudo.getNombre(),
					mensaje.getIdentificadorMensaje(), e.getMessage());
		}
	}

	private NuevoDocumentoReqModel datosDe(BuzonCorreo buzon, MensajeCorreoEntrante mensaje,
			CorrelacionCorreo correlacion) {
		NuevoDocumentoReqModel datos = new NuevoDocumentoReqModel();
		datos.setOrigen(OrigenDocumento.EMAIL);
		datos.setRemitente(mensaje.getRemitente());
		datos.setObservacion(recortar("Recibido por el buzon " + buzon.getDireccion() + " con asunto "
				+ mensaje.getAsunto(), 1024));
		datos.setCodigoPlantilla(correlacion != null && correlacion.getCodigoPlantilla() != null
				? correlacion.getCodigoPlantilla() : buzon.getCodigoPlantillaPorDefecto());
		if (correlacion != null && correlacion.getSujeto() != null) {
			datos.setSujetoOrigen(correlacion.getSujeto().getOrigen());
			datos.setSujetoTipoObjeto(correlacion.getSujeto().getTipoObjeto());
			datos.setSujetoIdObjeto(correlacion.getSujeto().getIdObjeto());
		}
		return datos;
	}

	private void abrirExcepcionesDeAsociacion(BuzonCorreo buzon, List<AdjuntoCorreo> adjuntos, String problema) {
		for (AdjuntoCorreo adjunto : adjuntos) {
			if (adjunto.getDocumento() == null || adjunto.getResultado() != ResultadoAdjuntoCorreo.INGESTADO) {
				continue;
			}
			excepcionDocumentalService.abrir(buzon.getTenant(), adjunto.getDocumento(), TipoExcepcion.ASOCIACION,
					SeveridadHallazgo.ADVERTENCIA, CODIGO_SIN_CORRELACION, problema);
		}
	}

	private MensajeCorreoModel cerrarSinIngesta(BuzonCorreo buzon, MensajeCorreoEntrante mensaje,
			ResultadoMensajeCorreo resultado, String motivo, PlantillaCorreo aviso) {
		mensaje.setResultado(resultado);
		mensaje.setMotivo(motivo);
		mensajeCorreoEntranteRepository.save(mensaje);
		auditar(buzon, mensaje, List.of());
		publicar(buzon, mensaje);
		if (aviso != null && buzon.isAcusarRecibo() && mensaje.getRemitente() != null) {
			correoSalienteService.enviar(buzon, null, aviso, mensaje.getRemitente(),
					"No pudimos procesar tu correo", cuerpoDeAviso(motivo, mensaje));
		}
		log.info("Mensaje {} del buzon {} cerrado sin ingesta: {}", mensaje.getIdentificadorMensaje(),
				buzon.getDireccion(), resultado);
		return canalCorreoConverter.aModelo(mensaje, List.of());
	}

	private ResultadoMensajeCorreo resultadoDe(String problemaCorrelacion, int adjuntos, int ingestados) {
		if (problemaCorrelacion != null) {
			return ResultadoMensajeCorreo.SIN_CORRELACION;
		}
		if (ingestados == 0) {
			return ResultadoMensajeCorreo.RECHAZADO;
		}
		return ingestados == adjuntos ? ResultadoMensajeCorreo.INGESTADO : ResultadoMensajeCorreo.PARCIAL;
	}

	private void responderSiCorresponde(BuzonCorreo buzon, MensajeCorreoEntrante mensaje,
			CorrelacionCorreo correlacion, int ingestados, int rechazados) {
		if (!buzon.isAcusarRecibo() || mensaje.getRemitente() == null) {
			return;
		}
		PlantillaCorreo plantilla = rechazados > 0 ? PlantillaCorreo.AVISO_ADJUNTO_RECHAZADO
				: PlantillaCorreo.ACUSE_RECIBO;
		if (mensaje.getResultado() == ResultadoMensajeCorreo.SIN_CORRELACION) {
			plantilla = PlantillaCorreo.AVISO_SIN_CORRELACION;
		}
		StringBuilder cuerpo = new StringBuilder();
		cuerpo.append("Recibimos tu correo \"").append(mensaje.getAsunto()).append("\".\n\n");
		cuerpo.append("Documentos ingresados: ").append(ingestados).append('\n');
		cuerpo.append("Adjuntos no procesados: ").append(rechazados).append('\n');
		if (mensaje.getMotivo() != null) {
			cuerpo.append('\n').append(mensaje.getMotivo()).append('\n');
		}
		cuerpo.append("\nIdentificador de seguimiento: ").append(mensaje.getCorrelacionTraza()).append('\n');
		correoSalienteService.enviar(buzon, correlacion, plantilla, mensaje.getRemitente(),
				"Acuse de recepcion de documentacion", cuerpo.toString());
	}

	private String cuerpoDeAviso(String motivo, MensajeCorreoEntrante mensaje) {
		return "No pudimos procesar el correo \"" + mensaje.getAsunto() + "\".\n\n" + motivo
				+ "\n\nIdentificador de seguimiento: " + mensaje.getCorrelacionTraza() + "\n";
	}

	private void auditar(BuzonCorreo buzon, MensajeCorreoEntrante mensaje, List<AdjuntoCorreo> adjuntos) {
		Map<String, Object> detalle = new LinkedHashMap<>();
		detalle.put("buzon", buzon.getDireccion());
		detalle.put("identificadorMensaje", mensaje.getIdentificadorMensaje());
		detalle.put("remitente", mensaje.getRemitente());
		detalle.put("resultado", mensaje.getResultado().name());
		detalle.put("token", mensaje.getTokenDetectado());
		detalle.put("adjuntos", mensaje.getAdjuntos());
		detalle.put("ingestados", mensaje.getIngestados());
		detalle.put("rechazados", mensaje.getRechazados());
		if (mensaje.getMotivo() != null) {
			detalle.put("motivo", mensaje.getMotivo());
		}
		if (!adjuntos.isEmpty()) {
			detalle.put("detalle", adjuntos.stream()
					.map(adjunto -> adjunto.getNombreArchivo() + "=" + adjunto.getResultado().name()).toList());
		}
		if (mensaje.getResultado() == ResultadoMensajeCorreo.INGESTADO
				|| mensaje.getResultado() == ResultadoMensajeCorreo.PARCIAL) {
			auditoriaService.registrarConDetalle(buzon.getTenant().getId(), AccionAuditoria.CORREO_RECIBIDO, ENTIDAD,
					mensaje.getId(), detalle);
		}
		else {
			auditoriaService.registrarFallo(buzon.getTenant().getId(), AccionAuditoria.CORREO_RECIBIDO, ENTIDAD,
					mensaje.getId(), detalle);
		}
	}

	private void publicar(BuzonCorreo buzon, MensajeCorreoEntrante mensaje) {
		Map<String, Object> carga = new LinkedHashMap<>();
		carga.put("buzon", buzon.getDireccion());
		carga.put("remitente", mensaje.getRemitente());
		carga.put("asunto", mensaje.getAsunto());
		carga.put("resultado", mensaje.getResultado().name());
		carga.put("token", mensaje.getTokenDetectado());
		carga.put("documentosIngestados", mensaje.getIngestados());
		carga.put("adjuntosRechazados", mensaje.getRechazados());
		boolean exitoso = mensaje.getResultado() == ResultadoMensajeCorreo.INGESTADO
				|| mensaje.getResultado() == ResultadoMensajeCorreo.PARCIAL;
		eventoSalidaService.publicar(buzon.getTenant().getId(),
				exitoso ? TipoEventoCanonico.CORREO_RECIBIDO : TipoEventoCanonico.CORREO_RECHAZADO, ENTIDAD,
				mensaje.getId(), carga);
	}

	private void marcarRechazo(AdjuntoCorreo adjunto, String codigo, String motivo) {
		adjunto.setResultado(ResultadoAdjuntoCorreo.RECHAZADO);
		adjunto.setCodigoRechazo(codigo);
		adjunto.setMotivo(recortar(motivo, 400));
	}

	private String recortar(String valor, int largo) {
		if (valor == null) {
			return null;
		}
		return valor.length() <= largo ? valor : valor.substring(0, largo);
	}
}
