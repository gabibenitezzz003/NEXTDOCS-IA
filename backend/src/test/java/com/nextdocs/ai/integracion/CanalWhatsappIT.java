package com.nextdocs.ai.integracion;

import static com.nextdocs.ai.integracion.FabricaDatosPrueba.campo;
import static com.nextdocs.ai.integracion.FabricaDatosPrueba.construirPdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.config.PropiedadesWhatsapp;
import com.nextdocs.ai.entidades.CorrelacionWhatsapp;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.MediaWhatsapp;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.EstadoLineaWhatsapp;
import com.nextdocs.ai.enumeraciones.EstadoMensajeSaliente;
import com.nextdocs.ai.enumeraciones.OrigenDocumento;
import com.nextdocs.ai.enumeraciones.ResultadoMediaWhatsapp;
import com.nextdocs.ai.enumeraciones.ResultadoMensajeWhatsapp;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.enumeraciones.TipoExcepcion;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.NoAutorizadoException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.ContactoWhatsappModel;
import com.nextdocs.ai.modelos.CorrelacionWhatsappModel;
import com.nextdocs.ai.modelos.LineaWhatsappModel;
import com.nextdocs.ai.modelos.MensajeWhatsappModel;
import com.nextdocs.ai.modelos.MensajeWhatsappSalienteModel;
import com.nextdocs.ai.modelos.NuevaCorrelacionWhatsappReqModel;
import com.nextdocs.ai.modelos.NuevaLineaWhatsappReqModel;
import com.nextdocs.ai.repositorios.CorrelacionWhatsappRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.ExcepcionDocumentalRepository;
import com.nextdocs.ai.repositorios.MediaWhatsappRepository;
import com.nextdocs.ai.servicios.ClienteWhatsappService;
import com.nextdocs.ai.servicios.DocumentoService;
import com.nextdocs.ai.servicios.LineaWhatsappService;
import com.nextdocs.ai.servicios.WebhookWhatsappService;
import com.nextdocs.ai.utiles.Hash;
import com.nextdocs.ai.utiles.TokenCorrelacion;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class CanalWhatsappIT extends PruebaIntegracion {

	private static final String SECRETO_APLICACION = "secreto-de-aplicacion-de-prueba";

	private static final String TOKEN_VERIFICACION = "token-de-verificacion-de-prueba";

	private static final String NUMERO_CONTACTO = "+5491133224455";

	private static final String NUMERO_AJENO = "+5491199887766";

	private static final AtomicInteger SECUENCIA = new AtomicInteger();

	private static ServidorGraphFalso graph;

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private LineaWhatsappService lineaWhatsappService;

	@Autowired
	private WebhookWhatsappService webhookWhatsappService;

	@Autowired
	private PropiedadesWhatsapp propiedades;

	@Autowired
	private DocumentoRepository documentoRepository;

	@Autowired
	private DocumentoService documentoService;

	@Autowired
	private MediaWhatsappRepository mediaWhatsappRepository;

	@Autowired
	private CorrelacionWhatsappRepository correlacionWhatsappRepository;

	@Autowired
	private ExcepcionDocumentalRepository excepcionDocumentalRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private Tenant tenant;

	private LineaWhatsappModel linea;

	private String identificadorNumero;

	private String urlOriginal;

	private String versionOriginal;

	@BeforeAll
	static void levantarGraph() throws Exception {
		graph = new ServidorGraphFalso();
		System.setProperty("NEXTDOCS_PRUEBA_WA_TOKEN", ServidorGraphFalso.TOKEN_ESPERADO);
		System.setProperty("NEXTDOCS_PRUEBA_WA_SECRETO", SECRETO_APLICACION);
		System.setProperty("NEXTDOCS_PRUEBA_WA_VERIFICACION", TOKEN_VERIFICACION);
	}

	@AfterAll
	static void bajarGraph() {
		graph.detener();
		System.clearProperty("NEXTDOCS_PRUEBA_WA_TOKEN");
		System.clearProperty("NEXTDOCS_PRUEBA_WA_SECRETO");
		System.clearProperty("NEXTDOCS_PRUEBA_WA_VERIFICACION");
	}

	@BeforeEach
	void prepararEscenario() {
		urlOriginal = propiedades.getUrlGraph();
		versionOriginal = propiedades.getVersionGraph();
		propiedades.setUrlGraph(graph.url());
		propiedades.setVersionGraph("v21.0");

		String sufijo = UUID.randomUUID().toString().substring(0, 8);
		tenant = fabrica.crearTenant("whatsapp-" + sufijo);
		fabrica.crearPlantillaPublicada(tenant, "REMITO",
				List.of(campo("numero", TipoDatoCampo.TEXTO, true)), List.of(), new BigDecimal("0.9500"));
		identificadorNumero = "numero-" + sufijo;
		linea = crearLinea(identificadorNumero, true, false);
	}

	@AfterEach
	void restaurarPropiedades() {
		propiedades.setUrlGraph(urlOriginal);
		propiedades.setVersionGraph(versionOriginal);
	}

	@Test
	@DisplayName("un documento con el token en el epigrafe ingesta y queda asociado al sujeto de la solicitud")
	void elTokenEnElEpigrafeAsocia() {
		CorrelacionWhatsappModel correlacion = solicitar("FOLLOW", "Caso", "CASO-8801", false);
		autorizar(NUMERO_CONTACTO);
		String media = graph.registrarMedia("application/pdf", construirPdf(List.of("REMITO 8801")));

		MensajeWhatsappModel mensaje = entregarUnico(sobreDeDocumento(NUMERO_CONTACTO, media, "remito.pdf",
				"application/pdf", "Ahi va lo pedido " + TokenCorrelacion.etiquetar(correlacion.getToken())));

		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeWhatsapp.INGESTADO.name());
		assertThat(mensaje.getTokenDetectado()).isEqualTo(correlacion.getToken());
		assertThat(mensaje.getIngestados()).isEqualTo(1);

		Documento documento = documentoDe(mensaje);
		assertThat(documento.getOrigen()).isEqualTo(OrigenDocumento.WHATSAPP);
		assertThat(documento.getRemitente()).isEqualTo(NUMERO_CONTACTO);
		assertThat(documento.getReferenciaSujeto().getIdObjeto()).isEqualTo("CASO-8801");
		assertThat(documento.getReferenciaSujeto().getTipoObjeto()).isEqualTo("Caso");
		assertThat(documentoService.obtener(tenant.getId(), documento.getId()).getCodigoPlantilla())
				.isEqualTo("REMITO");

		CorrelacionWhatsapp actualizada = correlacionWhatsappRepository.findById(correlacion.getId()).orElseThrow();
		assertThat(actualizada.getDocumentosRecibidos()).isEqualTo(1);
		assertThat(actualizada.getNumeroVinculado()).isEqualTo(NUMERO_CONTACTO);

		LineaWhatsappModel actual = lineaWhatsappService.obtener(tenant.getId(), linea.getId());
		assertThat(actual.getUltimoMensaje()).isNotNull();
		assertThat(actual.getUltimoError()).isNull();
		assertThat(actual.getFallosConsecutivos()).isZero();
	}

	@Test
	@DisplayName("si la Cloud API no entrega la media el fallo queda en la linea y no solo en el mensaje")
	void laCaidaDeLaApiQuedaVisibleEnLaLinea() {
		autorizar(NUMERO_CONTACTO);
		String cuerpo = sobreDeDocumento(NUMERO_CONTACTO, "media-que-no-existe", "remito.pdf",
				"application/pdf", null);

		MensajeWhatsappModel mensaje = entregarUnico(cuerpo);

		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeWhatsapp.RECHAZADO.name());
		assertThat(mediaDe(mensaje).getCodigoRechazo())
				.isEqualTo(ClienteWhatsappService.CODIGO_MEDIA_NO_DISPONIBLE);

		LineaWhatsappModel actual = lineaWhatsappService.obtener(tenant.getId(), linea.getId());
		assertThat(actual.getFallosConsecutivos()).isEqualTo(1);
		assertThat(actual.getUltimoError()).isNotBlank();
	}

	@Test
	@DisplayName("el token llega en un mensaje y las fotos en el siguiente, y se asocian igual")
	void elTokenViajaEnUnMensajeYLasFotosEnElSiguiente() {
		CorrelacionWhatsappModel correlacion = solicitar("FOLLOW", "Caso", "CASO-9002", false);
		autorizar(NUMERO_CONTACTO);

		MensajeWhatsappModel aviso = entregarUnico(sobreDeTexto(NUMERO_CONTACTO,
				"Hola, mando el remito del " + TokenCorrelacion.etiquetar(correlacion.getToken())));
		assertThat(aviso.getResultado()).isEqualTo(ResultadoMensajeWhatsapp.SIN_MEDIA.name());

		String media = graph.registrarMedia("image/jpeg", imagenJpeg());
		MensajeWhatsappModel conFoto = entregarUnico(sobreDeImagen(NUMERO_CONTACTO, media, "image/jpeg", null));

		assertThat(conFoto.getResultado()).isEqualTo(ResultadoMensajeWhatsapp.INGESTADO.name());
		assertThat(conFoto.getTokenDetectado()).isNull();
		assertThat(documentoDe(conFoto).getReferenciaSujeto().getIdObjeto()).isEqualTo("CASO-9002");
	}

	@Test
	@DisplayName("con dos solicitudes abiertas para el mismo numero no se elige una en silencio")
	void dosSolicitudesAbiertasVanARevisionHumana() {
		autorizar(NUMERO_CONTACTO);
		vincular(solicitar("FOLLOW", "Caso", "CASO-1", false));
		vincular(solicitar("FOLLOW", "Caso", "CASO-2", false));

		String media = graph.registrarMedia("application/pdf", construirPdf(List.of("REMITO SIN TOKEN")));
		MensajeWhatsappModel mensaje = entregarUnico(
				sobreDeDocumento(NUMERO_CONTACTO, media, "remito.pdf", "application/pdf", null));

		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeWhatsapp.CORRELACION_AMBIGUA.name());
		assertThat(mensaje.getMotivo()).contains("2 solicitudes abiertas");
		assertThat(mensaje.getIngestados()).isEqualTo(1);

		Documento documento = documentoDe(mensaje);
		assertThat(sujetoDe(documento)).isNull();
		assertThat(excepcionDocumentalRepository.listarPorDocumento(documento.getId()))
				.anyMatch(excepcion -> excepcion.getTipo() == TipoExcepcion.ASOCIACION);
	}

	@Test
	@DisplayName("un token que no corresponde a una solicitud vigente ingesta pero abre una excepcion")
	void elTokenVencidoNoAsocia() {
		autorizar(NUMERO_CONTACTO);
		CorrelacionWhatsappModel correlacion = solicitar("FOLLOW", "Caso", "CASO-VENCIDO", false);
		CorrelacionWhatsapp entidad = correlacionWhatsappRepository.findById(correlacion.getId()).orElseThrow();
		entidad.setVenceEn(Instant.now().minus(1, ChronoUnit.DAYS));
		correlacionWhatsappRepository.save(entidad);

		String media = graph.registrarMedia("application/pdf", construirPdf(List.of("TARDE")));
		MensajeWhatsappModel mensaje = entregarUnico(sobreDeDocumento(NUMERO_CONTACTO, media, "remito.pdf",
				"application/pdf", TokenCorrelacion.etiquetar(correlacion.getToken())));

		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeWhatsapp.SIN_CORRELACION.name());
		Documento documento = documentoDe(mensaje);
		assertThat(sujetoDe(documento)).isNull();
		assertThat(excepcionDocumentalRepository.listarPorDocumento(documento.getId()))
				.anyMatch(excepcion -> excepcion.getCodigo().equals("WHATSAPP_SIN_CORRELACION"));
	}

	@Test
	@DisplayName("una firma que no corresponde al secreto de la aplicacion se rechaza sin tocar la ingesta")
	void laFirmaInvalidaSeRechaza() {
		autorizar(NUMERO_CONTACTO);
		String media = graph.registrarMedia("application/pdf", construirPdf(List.of("INTRUSO")));
		String cuerpo = sobreDeDocumento(NUMERO_CONTACTO, media, "remito.pdf", "application/pdf", null);

		assertThatThrownBy(() -> webhookWhatsappService.recibir(linea.getRutaWebhook(),
				"sha256=" + Hash.hmacSha256("otro-secreto", cuerpo.getBytes(StandardCharsets.UTF_8)),
				cuerpo.getBytes(StandardCharsets.UTF_8))).isInstanceOf(NoAutorizadoException.class)
						.hasMessageContaining("firma");
		assertThat(documentosDelTenant()).isEmpty();
	}

	@Test
	@DisplayName("un evento sin la cabecera de firma se rechaza")
	void elEventoSinFirmaSeRechaza() {
		String cuerpo = sobreDeTexto(NUMERO_CONTACTO, "hola");
		assertThatThrownBy(() -> webhookWhatsappService.recibir(linea.getRutaWebhook(), null,
				cuerpo.getBytes(StandardCharsets.UTF_8))).isInstanceOf(NoAutorizadoException.class)
						.hasMessageContaining("X-Hub-Signature-256");
	}

	@Test
	@DisplayName("la ruta del webhook no existe para una linea desconocida")
	void laRutaDesconocidaNoResuelveLinea() {
		String cuerpo = sobreDeTexto(NUMERO_CONTACTO, "hola");
		assertThatThrownBy(() -> webhookWhatsappService.recibir("ruta-inexistente", firmar(cuerpo),
				cuerpo.getBytes(StandardCharsets.UTF_8)))
						.isInstanceOf(EntidadNoEncontradaException.class);
	}

	@Test
	@DisplayName("la verificacion de la suscripcion devuelve el desafio solo con el token correcto")
	void laVerificacionDevuelveElDesafio() {
		assertThat(webhookWhatsappService.verificarSuscripcion(linea.getRutaWebhook(), "subscribe",
				TOKEN_VERIFICACION, "desafio-123")).isEqualTo("desafio-123");

		assertThatThrownBy(() -> webhookWhatsappService.verificarSuscripcion(linea.getRutaWebhook(), "subscribe",
				"token-que-no-es", "desafio-123")).isInstanceOf(NoAutorizadoException.class);
	}

	@Test
	@DisplayName("un evento de otro numero de telefono no se procesa en la ruta de esta linea")
	void elEventoDeOtroNumeroSeDescarta() {
		autorizar(NUMERO_CONTACTO);
		String media = graph.registrarMedia("application/pdf", construirPdf(List.of("AJENO")));
		String cuerpo = sobre("numero-de-otra-linea", wamid(), NUMERO_CONTACTO, "document",
				Map.of("id", media, "filename", "remito.pdf", "mime_type", "application/pdf"));

		assertThat(entregar(cuerpo)).isEmpty();
		assertThat(documentosDelTenant()).isEmpty();
	}

	@Test
	@DisplayName("un numero fuera de la lista de contactos autorizados no ingesta pero queda en la bandeja")
	void elContactoNoAutorizadoQuedaRegistrado() {
		autorizar(NUMERO_CONTACTO);
		String media = graph.registrarMedia("application/pdf", construirPdf(List.of("DESCONOCIDO")));

		MensajeWhatsappModel mensaje = entregarUnico(
				sobreDeDocumento(NUMERO_AJENO, media, "remito.pdf", "application/pdf", null));

		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeWhatsapp.CONTACTO_NO_AUTORIZADO.name());
		assertThat(mensaje.getMotivo()).contains("no esta en la lista de contactos autorizados");
		assertThat(documentosDelTenant()).isEmpty();
		assertThat(lineaWhatsappService.listarMensajes(tenant.getId(), null, PageRequest.of(0, 10)).getContent())
				.hasSize(1);
	}

	@Test
	@DisplayName("a un numero no autorizado no se le contesta: abrir la conversacion se paga y confirma la linea")
	void alContactoNoAutorizadoNoSeLeContesta() {
		LineaWhatsappModel callada = crearLinea("numero-callado-" + UUID.randomUUID().toString().substring(0, 8),
				true, false, true);
		String media = graph.registrarMedia("application/pdf", construirPdf(List.of("DESCONOCIDO")));
		String cuerpo = sobre(callada.getIdentificadorNumero(), wamid(), NUMERO_AJENO, "document",
				Map.of("id", media, "filename", "remito.pdf", "mime_type", "application/pdf"));

		List<MensajeWhatsappModel> procesados = webhookWhatsappService.recibir(callada.getRutaWebhook(),
				firmar(cuerpo), cuerpo.getBytes(StandardCharsets.UTF_8));

		assertThat(procesados.get(0).getResultado())
				.isEqualTo(ResultadoMensajeWhatsapp.CONTACTO_NO_AUTORIZADO.name());
		assertThat(lineaWhatsappService.listarSalientes(tenant.getId(), PageRequest.of(0, 10)).getContent())
				.isEmpty();
	}

	@Test
	@DisplayName("un prefijo autorizado alcanza para todo un rango de numeros")
	void elPrefijoAutorizadoAlcanza() {
		autorizar("+54911*");
		String media = graph.registrarMedia("application/pdf", construirPdf(List.of("PREFIJO")));

		MensajeWhatsappModel mensaje = entregarUnico(
				sobreDeDocumento(NUMERO_CONTACTO, media, "remito.pdf", "application/pdf", null));

		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeWhatsapp.INGESTADO.name());
	}

	@Test
	@DisplayName("una media que supera el tope se rechaza sin descargar el binario")
	void laMediaExcedidaNoSeDescarga() {
		autorizar(NUMERO_CONTACTO);
		int descargasPrevias = graph.descargas();
		String media = graph.registrarMedia("application/pdf", construirPdf(List.of("GRANDE")),
				propiedades.getTamanoMaximoMediaBytes() + 1);

		MensajeWhatsappModel mensaje = entregarUnico(
				sobreDeDocumento(NUMERO_CONTACTO, media, "enorme.pdf", "application/pdf", null));

		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeWhatsapp.RECHAZADO.name());
		assertThat(mediaDe(mensaje).getCodigoRechazo()).isEqualTo(ClienteWhatsappService.CODIGO_MEDIA_EXCEDIDA);
		assertThat(graph.descargas()).isEqualTo(descargasPrevias);
		assertThat(documentosDelTenant()).isEmpty();
	}

	@Test
	@DisplayName("un archivo que miente sobre su tipo real se rechaza y el motivo queda en la bandeja")
	void elArchivoQueMienteSeRechaza() {
		autorizar(NUMERO_CONTACTO);
		String media = graph.registrarMedia("application/pdf",
				"MZ esto no es un pdf".getBytes(StandardCharsets.UTF_8));

		MensajeWhatsappModel mensaje = entregarUnico(
				sobreDeDocumento(NUMERO_CONTACTO, media, "contrato.pdf", "application/pdf", null));

		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeWhatsapp.RECHAZADO.name());
		MediaWhatsapp media0 = mediaDe(mensaje);
		assertThat(media0.getResultado()).isEqualTo(ResultadoMediaWhatsapp.RECHAZADO);
		assertThat(media0.getCodigoRechazo()).isEqualTo("MIME_NO_PERMITIDO");
		assertThat(media0.getSha256()).isNotBlank();
	}

	@Test
	@DisplayName("el mismo identificador de mensaje no se procesa dos veces")
	void elMensajeRepetidoNoDuplica() {
		autorizar(NUMERO_CONTACTO);
		String media = graph.registrarMedia("application/pdf", construirPdf(List.of("UNICO")));
		String cuerpo = sobreDeDocumento(NUMERO_CONTACTO, media, "remito.pdf", "application/pdf", null);

		assertThat(entregar(cuerpo)).hasSize(1);
		assertThat(entregar(cuerpo)).isEmpty();
		assertThat(documentosDelTenant()).hasSize(1);
	}

	@Test
	@DisplayName("una linea pausada descarta los eventos entrantes")
	void laLineaPausadaDescartaElEvento() {
		autorizar(NUMERO_CONTACTO);
		lineaWhatsappService.cambiarEstado(tenant.getId(), linea.getId(), EstadoLineaWhatsapp.PAUSADO);
		String media = graph.registrarMedia("application/pdf", construirPdf(List.of("PAUSADA")));

		assertThat(entregar(sobreDeDocumento(NUMERO_CONTACTO, media, "remito.pdf", "application/pdf", null)))
				.isEmpty();
		assertThat(documentosDelTenant()).isEmpty();
	}

	@Test
	@DisplayName("si la linea no exige correlacion los documentos entran sin sujeto y sin excepcion")
	void sinCorrelacionExigidaIngestaLibre() {
		LineaWhatsappModel libre = crearLinea("numero-libre-" + UUID.randomUUID().toString().substring(0, 8),
				false, false);
		String media = graph.registrarMedia("application/pdf", construirPdf(List.of("LIBRE")));
		String cuerpo = sobre(libre.getIdentificadorNumero(), wamid(), NUMERO_AJENO, "document",
				Map.of("id", media, "filename", "remito.pdf", "mime_type", "application/pdf"));

		List<MensajeWhatsappModel> procesados = webhookWhatsappService.recibir(libre.getRutaWebhook(),
				firmar(cuerpo), cuerpo.getBytes(StandardCharsets.UTF_8));

		assertThat(procesados).hasSize(1);
		assertThat(procesados.get(0).getResultado()).isEqualTo(ResultadoMensajeWhatsapp.INGESTADO.name());
		Documento documento = documentoDe(procesados.get(0));
		assertThat(sujetoDe(documento)).isNull();
		assertThat(excepcionDocumentalRepository.listarPorDocumento(documento.getId())).isEmpty();
	}

	@Test
	@DisplayName("si la linea exige correlacion un envio sin token queda sin asociar con su excepcion")
	void laLineaQueExigeCorrelacionAbreExcepcion() {
		LineaWhatsappModel estricta = crearLinea("numero-estricto-" + UUID.randomUUID().toString().substring(0, 8),
				false, true);
		String media = graph.registrarMedia("application/pdf", construirPdf(List.of("SIN TOKEN")));
		String cuerpo = sobre(estricta.getIdentificadorNumero(), wamid(), NUMERO_AJENO, "document",
				Map.of("id", media, "filename", "remito.pdf", "mime_type", "application/pdf"));

		List<MensajeWhatsappModel> procesados = webhookWhatsappService.recibir(estricta.getRutaWebhook(),
				firmar(cuerpo), cuerpo.getBytes(StandardCharsets.UTF_8));

		assertThat(procesados.get(0).getResultado()).isEqualTo(ResultadoMensajeWhatsapp.SIN_CORRELACION.name());
		assertThat(excepcionDocumentalRepository.listarPorDocumento(documentoDe(procesados.get(0)).getId()))
				.anyMatch(excepcion -> excepcion.getCodigo().equals("WHATSAPP_SIN_CORRELACION"));
	}

	@Test
	@DisplayName("fuera de la ventana de 24 horas no se manda texto libre y el motivo queda registrado")
	void fueraDeLaVentanaNoSeMandaTextoLibre() {
		CorrelacionWhatsappModel correlacion = solicitar("FOLLOW", "Caso", "CASO-FRIO", true);

		MensajeWhatsappSalienteModel saliente = lineaWhatsappService
				.listarSalientes(tenant.getId(), PageRequest.of(0, 10)).getContent().get(0);
		assertThat(saliente.getEstado()).isEqualTo(EstadoMensajeSaliente.FALLIDO.name());
		assertThat(saliente.isDentroDeVentana()).isFalse();
		assertThat(saliente.getDetalleError()).contains("plantilla");
		assertThat(correlacion.getMensajeSalienteId()).isEqualTo(saliente.getId());
	}

	@Test
	@DisplayName("dentro de la ventana de 24 horas el acuse sale como texto libre por la Cloud API")
	void dentroDeLaVentanaSeAcusaRecibo() {
		LineaWhatsappModel conAcuse = crearLinea("numero-acuse-" + UUID.randomUUID().toString().substring(0, 8),
				false, false, true);
		String media = graph.registrarMedia("application/pdf", construirPdf(List.of("ACUSE")));
		String cuerpo = sobre(conAcuse.getIdentificadorNumero(), wamid(), NUMERO_CONTACTO, "document",
				Map.of("id", media, "filename", "remito.pdf", "mime_type", "application/pdf"));
		webhookWhatsappService.recibir(conAcuse.getRutaWebhook(), firmar(cuerpo),
				cuerpo.getBytes(StandardCharsets.UTF_8));

		MensajeWhatsappSalienteModel saliente = lineaWhatsappService
				.listarSalientes(tenant.getId(), PageRequest.of(0, 10)).getContent().get(0);
		assertThat(saliente.getEstado()).isEqualTo(EstadoMensajeSaliente.ENVIADO.name());
		assertThat(saliente.isDentroDeVentana()).isTrue();
		assertThat(saliente.getIdentificadorMensaje()).startsWith("wamid.salida.");
		assertThat(graph.enviados()).anyMatch(enviado -> enviado.contains("\"type\":\"text\""));
	}

	@Test
	@DisplayName("un tenant no ve los mensajes de la linea de otro tenant")
	void unTenantNoVeLosMensajesDeOtro() {
		autorizar(NUMERO_CONTACTO);
		String media = graph.registrarMedia("application/pdf", construirPdf(List.of("PRIVADO")));
		entregarUnico(sobreDeDocumento(NUMERO_CONTACTO, media, "remito.pdf", "application/pdf", null));

		Tenant ajeno = fabrica.crearTenant("whatsapp-ajeno-" + UUID.randomUUID().toString().substring(0, 8));
		assertThat(lineaWhatsappService.listarMensajes(ajeno.getId(), null, PageRequest.of(0, 10)).getContent())
				.isEmpty();
		assertThat(lineaWhatsappService.listar(ajeno.getId())).isEmpty();
	}

	@Test
	@DisplayName("no se puede dar de alta una linea con un numero que no es E.164 ni con secretos sin resolver")
	void elAltaValidaNumeroYSecretos() {
		NuevaLineaWhatsappReqModel datos = datosDeLinea("numero-invalido");
		datos.setNumeroTelefono("11 2233-4455");
		assertThatThrownBy(() -> lineaWhatsappService.crear(tenant, datos)).isInstanceOf(ValidacionException.class)
				.hasMessageContaining("E.164");

		NuevaLineaWhatsappReqModel sinSecreto = datosDeLinea("numero-sin-secreto");
		sinSecreto.setReferenciaSecretoAplicacion("env:VARIABLE_QUE_NO_EXISTE");
		assertThatThrownBy(() -> lineaWhatsappService.crear(tenant, sinSecreto))
				.isInstanceOf(ValidacionException.class).hasMessageContaining("secreto de aplicacion");
	}

	private LineaWhatsappModel crearLinea(String identificador, boolean exigirContacto, boolean exigirCorrelacion) {
		return crearLinea(identificador, exigirContacto, exigirCorrelacion, false);
	}

	private LineaWhatsappModel crearLinea(String identificador, boolean exigirContacto, boolean exigirCorrelacion,
			boolean acusarRecibo) {
		NuevaLineaWhatsappReqModel datos = datosDeLinea(identificador);
		datos.setExigirContactoAutorizado(exigirContacto);
		datos.setExigirCorrelacion(exigirCorrelacion);
		datos.setAcusarRecibo(acusarRecibo);
		return lineaWhatsappService.crear(tenant, datos);
	}

	private NuevaLineaWhatsappReqModel datosDeLinea(String identificador) {
		NuevaLineaWhatsappReqModel datos = new NuevaLineaWhatsappReqModel();
		datos.setNombre("Mesa de entradas");
		datos.setNumeroTelefono("+541150000000");
		datos.setIdentificadorNumero(identificador);
		datos.setIdentificadorCuenta("waba-de-prueba");
		datos.setReferenciaTokenAcceso("env:NEXTDOCS_PRUEBA_WA_TOKEN");
		datos.setReferenciaSecretoAplicacion("env:NEXTDOCS_PRUEBA_WA_SECRETO");
		datos.setReferenciaTokenVerificacion("env:NEXTDOCS_PRUEBA_WA_VERIFICACION");
		datos.setMaximoMediaPorMensaje(10);
		datos.setMinutosVentanaCorrelacion(1440);
		return datos;
	}

	private void autorizar(String patron) {
		ContactoWhatsappModel datos = new ContactoWhatsappModel();
		datos.setPatron(patron);
		datos.setDescripcion("Alta de prueba");
		lineaWhatsappService.autorizar(tenant, linea.getId(), datos);
	}

	private CorrelacionWhatsappModel solicitar(String origen, String tipoObjeto, String idObjeto,
			boolean enviarSolicitud) {
		NuevaCorrelacionWhatsappReqModel pedido = new NuevaCorrelacionWhatsappReqModel();
		pedido.setLineaId(linea.getId());
		pedido.setSujetoOrigen(origen);
		pedido.setSujetoTipoObjeto(tipoObjeto);
		pedido.setSujetoIdObjeto(idObjeto);
		pedido.setCodigoPlantilla("REMITO");
		pedido.setNumeroDestino(NUMERO_CONTACTO);
		pedido.setDiasVigencia(30);
		pedido.setEnviarSolicitud(enviarSolicitud);
		return lineaWhatsappService.crearCorrelacion(tenant, pedido);
	}

	private void vincular(CorrelacionWhatsappModel correlacion) {
		CorrelacionWhatsapp entidad = correlacionWhatsappRepository.findById(correlacion.getId()).orElseThrow();
		entidad.setNumeroVinculado(NUMERO_CONTACTO);
		entidad.setVinculadoEn(Instant.now());
		correlacionWhatsappRepository.save(entidad);
	}

	private MensajeWhatsappModel entregarUnico(String cuerpo) {
		List<MensajeWhatsappModel> procesados = entregar(cuerpo);
		assertThat(procesados).hasSize(1);
		return procesados.get(0);
	}

	private List<MensajeWhatsappModel> entregar(String cuerpo) {
		return webhookWhatsappService.recibir(linea.getRutaWebhook(), firmar(cuerpo),
				cuerpo.getBytes(StandardCharsets.UTF_8));
	}

	private String firmar(String cuerpo) {
		return "sha256=" + Hash.hmacSha256(SECRETO_APLICACION, cuerpo.getBytes(StandardCharsets.UTF_8));
	}

	private String sobreDeTexto(String desde, String texto) {
		return sobre(identificadorNumero, wamid(), desde, "text", Map.of("body", texto));
	}

	private String sobreDeDocumento(String desde, String media, String nombre, String mime, String epigrafe) {
		Map<String, Object> documento = new LinkedHashMap<>();
		documento.put("id", media);
		documento.put("filename", nombre);
		documento.put("mime_type", mime);
		if (epigrafe != null) {
			documento.put("caption", epigrafe);
		}
		return sobre(identificadorNumero, wamid(), desde, "document", documento);
	}

	private String sobreDeImagen(String desde, String media, String mime, String epigrafe) {
		Map<String, Object> imagen = new LinkedHashMap<>();
		imagen.put("id", media);
		imagen.put("mime_type", mime);
		if (epigrafe != null) {
			imagen.put("caption", epigrafe);
		}
		return sobre(identificadorNumero, wamid(), desde, "image", imagen);
	}

	private String sobre(String numeroDeLinea, String wamid, String desde, String tipo,
			Map<String, Object> contenido) {
		Map<String, Object> mensaje = new LinkedHashMap<>();
		mensaje.put("from", desde.replace("+", ""));
		mensaje.put("id", wamid);
		mensaje.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));
		mensaje.put("type", tipo);
		mensaje.put(tipo, contenido);

		Map<String, Object> valor = new LinkedHashMap<>();
		valor.put("messaging_product", "whatsapp");
		valor.put("metadata", Map.of("display_phone_number", "541150000000", "phone_number_id", numeroDeLinea));
		valor.put("contacts", List.of(Map.of("profile", Map.of("name", "Proveedor de prueba"), "wa_id",
				desde.replace("+", ""))));
		valor.put("messages", List.of(mensaje));

		Map<String, Object> raiz = Map.of("object", "whatsapp_business_account", "entry",
				List.of(Map.of("id", "waba-de-prueba", "changes",
						List.of(Map.of("value", valor, "field", "messages")))));
		try {
			return objectMapper.writeValueAsString(raiz);
		}
		catch (Exception e) {
			throw new IllegalStateException("No se pudo construir el evento de prueba", e);
		}
	}

	private String wamid() {
		return "wamid.PRUEBA" + SECUENCIA.incrementAndGet() + UUID.randomUUID().toString().substring(0, 8);
	}

	private List<Documento> documentosDelTenant() {
		return documentoRepository.findAll().stream()
				.filter(documento -> documento.getTenant().getId().equals(tenant.getId())).toList();
	}

	private Documento documentoDe(MensajeWhatsappModel mensaje) {
		MediaWhatsapp conDocumento = mediaWhatsappRepository.listarPorMensaje(mensaje.getId()).stream()
				.filter(media -> media.getDocumento() != null).findFirst()
				.orElseThrow(() -> new AssertionError("El mensaje no genero ningun documento"));
		return documentoRepository.findById(conDocumento.getDocumento().getId()).orElseThrow();
	}

	private MediaWhatsapp mediaDe(MensajeWhatsappModel mensaje) {
		return mediaWhatsappRepository.listarPorMensaje(mensaje.getId()).stream().findFirst()
				.orElseThrow(() -> new AssertionError("El mensaje no registro ninguna media"));
	}

	private String sujetoDe(Documento documento) {
		return documento.getReferenciaSujeto() == null ? null : documento.getReferenciaSujeto().getIdObjeto();
	}

	private byte[] imagenJpeg() {
		return new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00,
				0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, (byte) 0xFF, (byte) 0xD9 };
	}
}
