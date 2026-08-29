package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.nextdocs.ai.convertidores.CanalWhatsappConverter;
import com.nextdocs.ai.entidades.ContactoWhatsappAutorizado;
import com.nextdocs.ai.entidades.CorrelacionWhatsapp;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.LineaWhatsapp;
import com.nextdocs.ai.entidades.MediaWhatsapp;
import com.nextdocs.ai.entidades.MensajeWhatsappEntrante;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.OrigenDocumento;
import com.nextdocs.ai.enumeraciones.PlantillaWhatsapp;
import com.nextdocs.ai.enumeraciones.ResultadoMediaWhatsapp;
import com.nextdocs.ai.enumeraciones.ResultadoMensajeWhatsapp;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.enumeraciones.TipoExcepcion;
import com.nextdocs.ai.exceptions.ArchivoRechazadoException;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.MediaDescargadaModel;
import com.nextdocs.ai.modelos.MensajeWhatsappCrudoModel;
import com.nextdocs.ai.modelos.MensajeWhatsappModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.repositorios.ContactoWhatsappAutorizadoRepository;
import com.nextdocs.ai.repositorios.CorrelacionWhatsappRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.LineaWhatsappRepository;
import com.nextdocs.ai.repositorios.MediaWhatsappRepository;
import com.nextdocs.ai.repositorios.MensajeWhatsappEntranteRepository;
import com.nextdocs.ai.utiles.ArchivoEnMemoria;
import com.nextdocs.ai.utiles.ContextoCorrelacion;
import com.nextdocs.ai.utiles.Hash;
import com.nextdocs.ai.utiles.NumeroTelefono;
import com.nextdocs.ai.utiles.TokenCorrelacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IngestaWhatsappService {

	public static final String ENTIDAD = "MensajeWhatsappEntrante";

	public static final String CODIGO_SIN_CORRELACION = "WHATSAPP_SIN_CORRELACION";

	public static final String CODIGO_CORRELACION_AMBIGUA = "WHATSAPP_CORRELACION_AMBIGUA";

	private static final Logger log = LoggerFactory.getLogger(IngestaWhatsappService.class);

	private final MensajeWhatsappEntranteRepository mensajeWhatsappEntranteRepository;

	private final MediaWhatsappRepository mediaWhatsappRepository;

	private final CorrelacionWhatsappRepository correlacionWhatsappRepository;

	private final ContactoWhatsappAutorizadoRepository contactoWhatsappAutorizadoRepository;

	private final DocumentoRepository documentoRepository;

	private final LineaWhatsappRepository lineaWhatsappRepository;

	private final LineaWhatsappService lineaWhatsappService;

	private final ClienteWhatsappService clienteWhatsappService;

	private final IngestaDocumentalService ingestaDocumentalService;

	private final ExcepcionDocumentalService excepcionDocumentalService;

	private final WhatsappSalienteService whatsappSalienteService;

	private final AuditoriaService auditoriaService;

	private final EventoSalidaService eventoSalidaService;

	private final CanalWhatsappConverter canalWhatsappConverter;

	public IngestaWhatsappService(MensajeWhatsappEntranteRepository mensajeWhatsappEntranteRepository,
			MediaWhatsappRepository mediaWhatsappRepository,
			CorrelacionWhatsappRepository correlacionWhatsappRepository,
			ContactoWhatsappAutorizadoRepository contactoWhatsappAutorizadoRepository,
			DocumentoRepository documentoRepository, LineaWhatsappRepository lineaWhatsappRepository,
			LineaWhatsappService lineaWhatsappService, ClienteWhatsappService clienteWhatsappService,
			IngestaDocumentalService ingestaDocumentalService,
			ExcepcionDocumentalService excepcionDocumentalService,
			WhatsappSalienteService whatsappSalienteService, AuditoriaService auditoriaService,
			EventoSalidaService eventoSalidaService, CanalWhatsappConverter canalWhatsappConverter) {
		this.mensajeWhatsappEntranteRepository = mensajeWhatsappEntranteRepository;
		this.mediaWhatsappRepository = mediaWhatsappRepository;
		this.correlacionWhatsappRepository = correlacionWhatsappRepository;
		this.contactoWhatsappAutorizadoRepository = contactoWhatsappAutorizadoRepository;
		this.documentoRepository = documentoRepository;
		this.lineaWhatsappRepository = lineaWhatsappRepository;
		this.lineaWhatsappService = lineaWhatsappService;
		this.clienteWhatsappService = clienteWhatsappService;
		this.ingestaDocumentalService = ingestaDocumentalService;
		this.excepcionDocumentalService = excepcionDocumentalService;
		this.whatsappSalienteService = whatsappSalienteService;
		this.auditoriaService = auditoriaService;
		this.eventoSalidaService = eventoSalidaService;
		this.canalWhatsappConverter = canalWhatsappConverter;
	}

	public MensajeWhatsappModel procesar(LineaWhatsapp linea, MensajeWhatsappCrudoModel crudo) {
		Optional<MensajeWhatsappEntrante> yaProcesado = mensajeWhatsappEntranteRepository
				.buscarPorIdentificador(linea.getId(), crudo.getIdentificadorMensaje());
		if (yaProcesado.isPresent()) {
			log.debug("El mensaje {} de la linea {} ya estaba procesado", crudo.getIdentificadorMensaje(),
					linea.getNumeroTelefono());
			return null;
		}

		ContextoCorrelacion.establecer(UUID.randomUUID().toString());
		MensajeWhatsappEntrante mensaje = registrarRecepcion(linea, crudo);
		marcarActividad(linea, mensaje.getRecibidoEn());

		if (!estaAutorizado(linea, mensaje.getNumeroOrigen())) {
			return cerrarSinIngesta(linea, mensaje, ResultadoMensajeWhatsapp.CONTACTO_NO_AUTORIZADO,
					"El numero " + NumeroTelefono.enmascarar(mensaje.getNumeroOrigen())
							+ " no esta en la lista de contactos autorizados de la linea");
		}

		String token = TokenCorrelacion.detectar(textosDe(crudo)).orElse(null);
		mensaje.setTokenDetectado(token);

		if (crudo.getMedia().isEmpty()) {
			vincularSiCorresponde(linea, mensaje, token);
			return cerrarSinIngesta(linea, mensaje, ResultadoMensajeWhatsapp.SIN_MEDIA,
					token == null ? "El mensaje no trae archivos para ingestar"
							: "El mensaje trae el token " + token
									+ " pero no adjunta archivos: queda a la espera de los documentos");
		}

		ResolucionCorrelacion resolucion = resolver(linea, mensaje.getNumeroOrigen(), token);
		mensaje.setCorrelacion(resolucion.correlacion());

		List<MediaWhatsapp> media = ingestar(linea, mensaje, crudo, resolucion.correlacion());
		int ingestados = (int) media.stream()
				.filter(elemento -> elemento.getResultado() == ResultadoMediaWhatsapp.INGESTADO).count();
		int rechazados = media.size() - ingestados;

		mensaje.setMedia(media.size());
		mensaje.setIngestados(ingestados);
		mensaje.setRechazados(rechazados);
		mensaje.setResultado(resultadoDe(resolucion, media.size(), ingestados));
		mensaje.setMotivo(resolucion.problema());
		mensajeWhatsappEntranteRepository.save(mensaje);

		if (resolucion.problema() != null) {
			abrirExcepcionesDeAsociacion(linea, media, resolucion);
		}
		else if (resolucion.correlacion() != null && ingestados > 0) {
			CorrelacionWhatsapp correlacion = resolucion.correlacion();
			correlacion.setDocumentosRecibidos(correlacion.getDocumentosRecibidos() + ingestados);
			correlacion.setUltimoUso(Instant.now());
			correlacion.setNumeroVinculado(mensaje.getNumeroOrigen());
			correlacion.setVinculadoEn(Instant.now());
			correlacionWhatsappRepository.save(correlacion);
		}

		registrarSalud(linea, media);
		auditar(linea, mensaje, media);
		publicar(linea, mensaje);
		responderSiCorresponde(linea, mensaje, resolucion.correlacion(), ingestados, rechazados);
		return canalWhatsappConverter.aModelo(mensaje, media);
	}

	private MensajeWhatsappEntrante registrarRecepcion(LineaWhatsapp linea, MensajeWhatsappCrudoModel crudo) {
		MensajeWhatsappEntrante mensaje = new MensajeWhatsappEntrante();
		mensaje.setTenant(linea.getTenant());
		mensaje.setLinea(linea);
		mensaje.setIdentificadorMensaje(crudo.getIdentificadorMensaje());
		mensaje.setNumeroOrigen(NumeroTelefono.normalizar(crudo.getNumeroOrigen()));
		mensaje.setNombrePerfil(recortar(crudo.getNombrePerfil(), 128));
		mensaje.setTipo(recortar(crudo.getTipo(), 32));
		mensaje.setTexto(recortar(crudo.getTexto(), 1024));
		mensaje.setResultado(ResultadoMensajeWhatsapp.ERROR);
		mensaje.setMotivo("El procesamiento del mensaje no termino");
		mensaje.setCorrelacionTraza(ContextoCorrelacion.obtenerOGenerar());
		mensaje.setRecibidoEn(crudo.getRecibidoEn() == null ? Instant.now() : crudo.getRecibidoEn());
		mensaje.setAlta(Instant.now());
		return mensajeWhatsappEntranteRepository.save(mensaje);
	}

	private void registrarSalud(LineaWhatsapp linea, List<MediaWhatsapp> media) {
		Optional<MediaWhatsapp> caida = media.stream()
				.filter(elemento -> ClienteWhatsappService.CODIGO_MEDIA_NO_DISPONIBLE
						.equals(elemento.getCodigoRechazo()))
				.findFirst();
		if (caida.isPresent()) {
			lineaWhatsappService.registrarFallo(linea, caida.get().getMotivo());
			return;
		}
		if (media.stream().anyMatch(elemento -> elemento.getResultado() == ResultadoMediaWhatsapp.INGESTADO)) {
			lineaWhatsappService.registrarLineaSana(linea);
		}
	}

	private void marcarActividad(LineaWhatsapp linea, Instant recibidoEn) {
		if (linea.getUltimoMensaje() != null && linea.getUltimoMensaje().isAfter(recibidoEn)) {
			return;
		}
		linea.setUltimoMensaje(recibidoEn);
		lineaWhatsappRepository.save(linea);
	}

	private boolean estaAutorizado(LineaWhatsapp linea, String numero) {
		if (!linea.isExigirContactoAutorizado()) {
			return true;
		}
		if (numero == null) {
			return false;
		}
		List<ContactoWhatsappAutorizado> autorizados = contactoWhatsappAutorizadoRepository
				.listarPorLinea(linea.getId());
		return autorizados.stream().anyMatch(contacto -> NumeroTelefono.coincide(contacto.getPatron(), numero));
	}

	private String[] textosDe(MensajeWhatsappCrudoModel crudo) {
		List<String> textos = new ArrayList<>();
		textos.add(crudo.getTexto());
		for (MensajeWhatsappCrudoModel.MediaCrudaModel media : crudo.getMedia()) {
			textos.add(media.getDescripcion());
			textos.add(media.getNombreDeclarado());
		}
		return textos.toArray(new String[0]);
	}

	private ResolucionCorrelacion resolver(LineaWhatsapp linea, String numero, String token) {
		if (token != null) {
			Optional<CorrelacionWhatsapp> porToken = correlacionWhatsappRepository
					.buscarPorTokenYTenant(token, linea.getTenant().getId())
					.filter(candidata -> candidata.getLinea() != null
							&& candidata.getLinea().getId().equals(linea.getId()))
					.filter(candidata -> candidata.getVenceEn() == null
							|| candidata.getVenceEn().isAfter(Instant.now()));
			if (porToken.isPresent()) {
				return new ResolucionCorrelacion(porToken.get(), null, false);
			}
			return new ResolucionCorrelacion(null, "El token " + token
					+ " no corresponde a una solicitud vigente de esta linea: "
					+ "los documentos quedaron sin asociar para revision humana", false);
		}

		List<CorrelacionWhatsapp> vinculadas = numero == null ? List.of()
				: correlacionWhatsappRepository.listarVinculadasAlNumero(linea.getId(), numero,
						Instant.now().minus(ventanaDe(linea), ChronoUnit.MINUTES), Instant.now());
		if (vinculadas.size() == 1) {
			return new ResolucionCorrelacion(vinculadas.get(0), null, false);
		}
		if (vinculadas.size() > 1) {
			return new ResolucionCorrelacion(null,
					"El numero " + NumeroTelefono.enmascarar(numero) + " tiene " + vinculadas.size()
							+ " solicitudes abiertas y el mensaje no trae token: no se elige una en silencio,"
							+ " los documentos quedaron para revision humana",
					true);
		}
		if (linea.isExigirCorrelacion()) {
			return new ResolucionCorrelacion(null,
					"El mensaje no trae token de correlacion y la linea lo exige: "
							+ "los documentos quedaron sin asociar para revision humana",
					false);
		}
		return new ResolucionCorrelacion(null, null, false);
	}

	private int ventanaDe(LineaWhatsapp linea) {
		return linea.getMinutosVentanaCorrelacion() <= 0 ? 1440 : linea.getMinutosVentanaCorrelacion();
	}

	private void vincularSiCorresponde(LineaWhatsapp linea, MensajeWhatsappEntrante mensaje, String token) {
		if (token == null || mensaje.getNumeroOrigen() == null) {
			return;
		}
		correlacionWhatsappRepository.buscarPorTokenYTenant(token, linea.getTenant().getId())
				.filter(candidata -> candidata.getLinea() != null
						&& candidata.getLinea().getId().equals(linea.getId()))
				.filter(candidata -> candidata.getVenceEn() == null
						|| candidata.getVenceEn().isAfter(Instant.now()))
				.ifPresent(correlacion -> {
					correlacion.setNumeroVinculado(mensaje.getNumeroOrigen());
					correlacion.setVinculadoEn(Instant.now());
					correlacionWhatsappRepository.save(correlacion);
					mensaje.setCorrelacion(correlacion);
				});
	}

	private List<MediaWhatsapp> ingestar(LineaWhatsapp linea, MensajeWhatsappEntrante mensaje,
			MensajeWhatsappCrudoModel crudo, CorrelacionWhatsapp correlacion) {
		List<MediaWhatsapp> resultados = new ArrayList<>();
		int tope = linea.getMaximoMediaPorMensaje() <= 0 ? crudo.getMedia().size()
				: linea.getMaximoMediaPorMensaje();
		for (int indice = 0; indice < crudo.getMedia().size(); indice++) {
			MensajeWhatsappCrudoModel.MediaCrudaModel referencia = crudo.getMedia().get(indice);
			MediaWhatsapp media = new MediaWhatsapp();
			media.setTenant(linea.getTenant());
			media.setMensaje(mensaje);
			media.setIdentificadorMedia(recortar(referencia.getIdentificadorMedia(), 128));
			media.setNombreArchivo(recortar(referencia.getNombreDeclarado(), 400));
			media.setTipoMime(recortar(referencia.getTipoMimeDeclarado(), 128));
			media.setAlta(Instant.now());

			if (indice >= tope) {
				marcarRechazo(media, "DEMASIADA_MEDIA",
						"El mensaje supera el maximo de " + tope + " archivos configurado en la linea");
				resultados.add(mediaWhatsappRepository.save(media));
				continue;
			}
			MediaDescargadaModel descargada = clienteWhatsappService.descargar(linea,
					referencia.getIdentificadorMedia(), referencia.getNombreDeclarado());
			media.setNombreArchivo(recortar(descargada.getNombreArchivo(), 400));
			media.setTipoMime(recortar(descargada.getTipoMime(), 128));
			media.setTamanoBytes(descargada.estaDisponible() ? descargada.getContenido().length
					: descargada.getTamanoDeclarado());
			if (!descargada.estaDisponible()) {
				marcarRechazo(media, descargada.getCodigoRechazo() == null
						? ClienteWhatsappService.CODIGO_MEDIA_NO_DISPONIBLE : descargada.getCodigoRechazo(),
						descargada.getMotivo());
			}
			else {
				media.setSha256(Hash.sha256(descargada.getContenido()));
				aplicarIngesta(linea, mensaje, correlacion, descargada, media, indice);
			}
			resultados.add(mediaWhatsappRepository.save(media));
		}
		return resultados;
	}

	private void aplicarIngesta(LineaWhatsapp linea, MensajeWhatsappEntrante mensaje, CorrelacionWhatsapp correlacion,
			MediaDescargadaModel descargada, MediaWhatsapp media, int indice) {
		try {
			DocumentoModel documento = ingestaDocumentalService.ingresar(linea.getTenant(), null,
					new ArchivoEnMemoria(descargada.getNombreArchivo(), descargada.getTipoMime(),
							descargada.getContenido()),
					datosDe(linea, mensaje, correlacion),
					"whatsapp:" + mensaje.getIdentificadorMensaje() + ":" + indice);
			Documento entidad = documentoRepository.findById(documento.getId()).orElse(null);
			media.setDocumento(entidad);
			if (entidad != null && entidad.getEstado() == EstadoDocumento.RECHAZADO) {
				media.setResultado(ResultadoMediaWhatsapp.EN_CUARENTENA);
				media.setCodigoRechazo(IngestaDocumentalService.CODIGO_ARCHIVO_INFECTADO);
				media.setMotivo(recortar(entidad.getObservacion(), 400));
			}
			else {
				media.setResultado(ResultadoMediaWhatsapp.INGESTADO);
			}
		}
		catch (ArchivoRechazadoException e) {
			marcarRechazo(media, e.getCodigo(), e.getMessage());
		}
		catch (Exception e) {
			media.setResultado(ResultadoMediaWhatsapp.ERROR);
			media.setCodigoRechazo("ERROR_INGESTA");
			media.setMotivo(recortar(e.getMessage(), 400));
			log.warn("Fallo la ingesta de la media {} del mensaje {}: {}", media.getIdentificadorMedia(),
					mensaje.getIdentificadorMensaje(), e.getMessage());
		}
	}

	private NuevoDocumentoReqModel datosDe(LineaWhatsapp linea, MensajeWhatsappEntrante mensaje,
			CorrelacionWhatsapp correlacion) {
		NuevoDocumentoReqModel datos = new NuevoDocumentoReqModel();
		datos.setOrigen(OrigenDocumento.WHATSAPP);
		datos.setRemitente(mensaje.getNumeroOrigen());
		datos.setObservacion(recortar("Recibido por la linea de WhatsApp " + linea.getNumeroTelefono() + " desde "
				+ mensaje.getNumeroOrigen()
				+ (mensaje.getNombrePerfil() == null ? "" : " (" + mensaje.getNombrePerfil() + ")"), 1024));
		datos.setCodigoPlantilla(correlacion != null && correlacion.getCodigoPlantilla() != null
				? correlacion.getCodigoPlantilla() : linea.getCodigoPlantillaPorDefecto());
		if (correlacion != null && correlacion.getSujeto() != null) {
			datos.setSujetoOrigen(correlacion.getSujeto().getOrigen());
			datos.setSujetoTipoObjeto(correlacion.getSujeto().getTipoObjeto());
			datos.setSujetoIdObjeto(correlacion.getSujeto().getIdObjeto());
		}
		return datos;
	}

	private void abrirExcepcionesDeAsociacion(LineaWhatsapp linea, List<MediaWhatsapp> media,
			ResolucionCorrelacion resolucion) {
		String codigo = resolucion.ambigua() ? CODIGO_CORRELACION_AMBIGUA : CODIGO_SIN_CORRELACION;
		for (MediaWhatsapp elemento : media) {
			if (elemento.getDocumento() == null || elemento.getResultado() != ResultadoMediaWhatsapp.INGESTADO) {
				continue;
			}
			excepcionDocumentalService.abrir(linea.getTenant(), elemento.getDocumento(), TipoExcepcion.ASOCIACION,
					SeveridadHallazgo.ADVERTENCIA, codigo, resolucion.problema());
		}
	}

	private MensajeWhatsappModel cerrarSinIngesta(LineaWhatsapp linea, MensajeWhatsappEntrante mensaje,
			ResultadoMensajeWhatsapp resultado, String motivo) {
		mensaje.setResultado(resultado);
		mensaje.setMotivo(motivo);
		mensajeWhatsappEntranteRepository.save(mensaje);
		auditar(linea, mensaje, List.of());
		publicar(linea, mensaje);
		log.info("Mensaje {} de la linea {} cerrado sin ingesta: {}", mensaje.getIdentificadorMensaje(),
				linea.getNumeroTelefono(), resultado);
		return canalWhatsappConverter.aModelo(mensaje, List.of());
	}

	private ResultadoMensajeWhatsapp resultadoDe(ResolucionCorrelacion resolucion, int media, int ingestados) {
		if (resolucion.ambigua()) {
			return ResultadoMensajeWhatsapp.CORRELACION_AMBIGUA;
		}
		if (resolucion.problema() != null) {
			return ResultadoMensajeWhatsapp.SIN_CORRELACION;
		}
		if (ingestados == 0) {
			return ResultadoMensajeWhatsapp.RECHAZADO;
		}
		return ingestados == media ? ResultadoMensajeWhatsapp.INGESTADO : ResultadoMensajeWhatsapp.PARCIAL;
	}

	private void responderSiCorresponde(LineaWhatsapp linea, MensajeWhatsappEntrante mensaje,
			CorrelacionWhatsapp correlacion, int ingestados, int rechazados) {
		if (!linea.isAcusarRecibo() || mensaje.getNumeroOrigen() == null) {
			return;
		}
		PlantillaWhatsapp plantilla = rechazados > 0 ? PlantillaWhatsapp.AVISO_MEDIA_RECHAZADA
				: PlantillaWhatsapp.ACUSE_RECIBO;
		if (mensaje.getResultado() == ResultadoMensajeWhatsapp.SIN_CORRELACION
				|| mensaje.getResultado() == ResultadoMensajeWhatsapp.CORRELACION_AMBIGUA) {
			plantilla = PlantillaWhatsapp.AVISO_SIN_CORRELACION;
		}
		StringBuilder cuerpo = new StringBuilder();
		cuerpo.append("Recibimos tu envio.\n\n");
		cuerpo.append("Documentos ingresados: ").append(ingestados).append('\n');
		cuerpo.append("Archivos no procesados: ").append(rechazados).append('\n');
		if (mensaje.getMotivo() != null) {
			cuerpo.append('\n').append(mensaje.getMotivo()).append('\n');
		}
		cuerpo.append("\nSeguimiento: ").append(mensaje.getCorrelacionTraza());
		whatsappSalienteService.enviar(linea, correlacion, plantilla, mensaje.getNumeroOrigen(), cuerpo.toString(),
				List.of(String.valueOf(ingestados)));
	}

	private void auditar(LineaWhatsapp linea, MensajeWhatsappEntrante mensaje, List<MediaWhatsapp> media) {
		Map<String, Object> detalle = new LinkedHashMap<>();
		detalle.put("linea", linea.getNumeroTelefono());
		detalle.put("identificadorMensaje", mensaje.getIdentificadorMensaje());
		detalle.put("origen", NumeroTelefono.enmascarar(mensaje.getNumeroOrigen()));
		detalle.put("resultado", mensaje.getResultado().name());
		detalle.put("token", mensaje.getTokenDetectado());
		detalle.put("media", mensaje.getMedia());
		detalle.put("ingestados", mensaje.getIngestados());
		detalle.put("rechazados", mensaje.getRechazados());
		if (mensaje.getMotivo() != null) {
			detalle.put("motivo", mensaje.getMotivo());
		}
		if (!media.isEmpty()) {
			detalle.put("detalle", media.stream()
					.map(elemento -> elemento.getNombreArchivo() + "=" + elemento.getResultado().name()).toList());
		}
		if (mensaje.getResultado() == ResultadoMensajeWhatsapp.INGESTADO
				|| mensaje.getResultado() == ResultadoMensajeWhatsapp.PARCIAL) {
			auditoriaService.registrarConDetalle(linea.getTenant().getId(), AccionAuditoria.WHATSAPP_RECIBIDO,
					ENTIDAD, mensaje.getId(), detalle);
		}
		else {
			auditoriaService.registrarFallo(linea.getTenant().getId(), AccionAuditoria.WHATSAPP_RECIBIDO, ENTIDAD,
					mensaje.getId(), detalle);
		}
	}

	private void publicar(LineaWhatsapp linea, MensajeWhatsappEntrante mensaje) {
		Map<String, Object> carga = new LinkedHashMap<>();
		carga.put("linea", linea.getNumeroTelefono());
		carga.put("origen", NumeroTelefono.enmascarar(mensaje.getNumeroOrigen()));
		carga.put("resultado", mensaje.getResultado().name());
		carga.put("token", mensaje.getTokenDetectado());
		carga.put("documentosIngestados", mensaje.getIngestados());
		carga.put("archivosRechazados", mensaje.getRechazados());
		boolean exitoso = mensaje.getResultado() == ResultadoMensajeWhatsapp.INGESTADO
				|| mensaje.getResultado() == ResultadoMensajeWhatsapp.PARCIAL;
		eventoSalidaService.publicar(linea.getTenant().getId(),
				exitoso ? TipoEventoCanonico.WHATSAPP_RECIBIDO : TipoEventoCanonico.WHATSAPP_RECHAZADO, ENTIDAD,
				mensaje.getId(), carga);
	}

	private void marcarRechazo(MediaWhatsapp media, String codigo, String motivo) {
		media.setResultado(ResultadoMediaWhatsapp.RECHAZADO);
		media.setCodigoRechazo(codigo);
		media.setMotivo(recortar(motivo, 400));
	}

	private String recortar(String valor, int largo) {
		if (valor == null) {
			return null;
		}
		return valor.length() <= largo ? valor : valor.substring(0, largo);
	}

	private record ResolucionCorrelacion(CorrelacionWhatsapp correlacion, String problema, boolean ambigua) {
	}
}
