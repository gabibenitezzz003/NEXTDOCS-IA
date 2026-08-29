package com.nextdocs.ai.servicios;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.config.PropiedadesWhatsapp;
import com.nextdocs.ai.entidades.LineaWhatsapp;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoLineaWhatsapp;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.NoAutorizadoException;
import com.nextdocs.ai.modelos.MensajeWhatsappCrudoModel;
import com.nextdocs.ai.modelos.MensajeWhatsappModel;
import com.nextdocs.ai.repositorios.LineaWhatsappRepository;
import com.nextdocs.ai.utiles.Hash;
import com.nextdocs.ai.utiles.NumeroTelefono;
import com.nextdocs.ai.utiles.ResolvedorSecreto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookWhatsappService {

	public static final String CABECERA_FIRMA = "X-Hub-Signature-256";

	public static final String PREFIJO_FIRMA = "sha256=";

	public static final String MODO_SUSCRIPCION = "subscribe";

	private static final List<String> TIPOS_CON_MEDIA = List.of("document", "image");

	private static final Logger log = LoggerFactory.getLogger(WebhookWhatsappService.class);

	private final LineaWhatsappRepository lineaWhatsappRepository;

	private final IngestaWhatsappService ingestaWhatsappService;

	private final AuditoriaService auditoriaService;

	private final ObjectMapper objectMapper;

	private final PropiedadesWhatsapp propiedades;

	public WebhookWhatsappService(LineaWhatsappRepository lineaWhatsappRepository,
			IngestaWhatsappService ingestaWhatsappService, AuditoriaService auditoriaService,
			ObjectMapper objectMapper, PropiedadesWhatsapp propiedades) {
		this.lineaWhatsappRepository = lineaWhatsappRepository;
		this.ingestaWhatsappService = ingestaWhatsappService;
		this.auditoriaService = auditoriaService;
		this.objectMapper = objectMapper;
		this.propiedades = propiedades;
	}

	@Transactional(readOnly = true)
	public String verificarSuscripcion(String rutaWebhook, String modo, String tokenRecibido, String desafio) {
		LineaWhatsapp linea = buscarPorRuta(rutaWebhook);
		if (!MODO_SUSCRIPCION.equals(modo)) {
			throw new NoAutorizadoException("El modo de verificacion " + modo + " no esta soportado");
		}
		String esperado = ResolvedorSecreto.resolver(linea.getReferenciaTokenVerificacion())
				.orElseThrow(() -> new NoAutorizadoException("La linea " + linea.getNumeroTelefono()
						+ " no tiene resuelto el token de verificacion " + linea.getReferenciaTokenVerificacion()));
		if (!Hash.sonIguales(esperado, tokenRecibido)) {
			rechazar(linea, "El token de verificacion no coincide");
		}
		log.info("Suscripcion verificada para la linea {}", linea.getNumeroTelefono());
		return desafio;
	}

	public List<MensajeWhatsappModel> recibir(String rutaWebhook, String firma, byte[] cuerpo) {
		LineaWhatsapp linea = buscarPorRuta(rutaWebhook);
		exigirFirma(linea, firma, cuerpo);
		if (!propiedades.isActivo()) {
			log.warn("El canal de WhatsApp esta apagado por configuracion y descarta el evento de la linea {}",
					linea.getNumeroTelefono());
			return List.of();
		}
		if (linea.getEstado() != EstadoLineaWhatsapp.ACTIVO) {
			log.info("La linea {} esta {} y descarta el evento entrante", linea.getNumeroTelefono(),
					linea.getEstado());
			return List.of();
		}

		List<MensajeWhatsappModel> procesados = new ArrayList<>();
		for (MensajeWhatsappCrudoModel crudo : interpretar(linea, cuerpo)) {
			MensajeWhatsappModel modelo = ingestaWhatsappService.procesar(linea, crudo);
			if (modelo != null) {
				procesados.add(modelo);
			}
		}
		return procesados;
	}

	public List<MensajeWhatsappCrudoModel> interpretar(LineaWhatsapp linea, byte[] cuerpo) {
		List<MensajeWhatsappCrudoModel> mensajes = new ArrayList<>();
		JsonNode raiz;
		try {
			raiz = objectMapper.readTree(cuerpo);
		}
		catch (Exception e) {
			log.warn("El webhook de la linea {} trajo un cuerpo ilegible", linea.getNumeroTelefono());
			return mensajes;
		}
		for (JsonNode entrada : raiz.path("entry")) {
			for (JsonNode cambio : entrada.path("changes")) {
				JsonNode valor = cambio.path("value");
				String identificadorNumero = valor.path("metadata").path("phone_number_id").asText(null);
				if (!coincideLaLinea(linea, identificadorNumero)) {
					continue;
				}
				Map<String, String> perfiles = perfilesDe(valor);
				for (JsonNode mensaje : valor.path("messages")) {
					MensajeWhatsappCrudoModel crudo = convertir(mensaje, identificadorNumero, perfiles);
					if (crudo != null) {
						mensajes.add(crudo);
					}
				}
			}
		}
		return mensajes;
	}

	private boolean coincideLaLinea(LineaWhatsapp linea, String identificadorNumero) {
		if (identificadorNumero == null || identificadorNumero.isBlank()) {
			return false;
		}
		if (identificadorNumero.equals(linea.getIdentificadorNumero())) {
			return true;
		}
		log.warn("La ruta de webhook de la linea {} recibio un evento del numero {} y se descarta",
				linea.getNumeroTelefono(), identificadorNumero);
		return false;
	}

	private Map<String, String> perfilesDe(JsonNode valor) {
		Map<String, String> perfiles = new java.util.LinkedHashMap<>();
		for (JsonNode contacto : valor.path("contacts")) {
			String identificador = contacto.path("wa_id").asText(null);
			String nombre = contacto.path("profile").path("name").asText(null);
			if (identificador != null && nombre != null) {
				perfiles.put(identificador, nombre);
			}
		}
		return perfiles;
	}

	private MensajeWhatsappCrudoModel convertir(JsonNode mensaje, String identificadorNumero,
			Map<String, String> perfiles) {
		String identificador = mensaje.path("id").asText(null);
		if (identificador == null || identificador.isBlank()) {
			return null;
		}
		String tipo = mensaje.path("type").asText("desconocido");
		String origen = mensaje.path("from").asText(null);

		MensajeWhatsappCrudoModel crudo = new MensajeWhatsappCrudoModel();
		crudo.setIdentificadorMensaje(identificador);
		crudo.setIdentificadorNumero(identificadorNumero);
		crudo.setNumeroOrigen(NumeroTelefono.normalizar(origen));
		crudo.setNombrePerfil(perfiles.get(origen));
		crudo.setTipo(tipo);
		crudo.setRecibidoEn(instanteDe(mensaje.path("timestamp").asText(null)));
		crudo.setTexto(textoDe(mensaje, tipo));

		if (TIPOS_CON_MEDIA.contains(tipo)) {
			JsonNode media = mensaje.path(tipo);
			String identificadorMedia = media.path("id").asText(null);
			if (identificadorMedia != null && !identificadorMedia.isBlank()) {
				MensajeWhatsappCrudoModel.MediaCrudaModel referencia = new MensajeWhatsappCrudoModel.MediaCrudaModel();
				referencia.setIdentificadorMedia(identificadorMedia);
				referencia.setNombreDeclarado(media.path("filename").asText(null));
				referencia.setTipoMimeDeclarado(media.path("mime_type").asText(null));
				referencia.setDescripcion(media.path("caption").asText(null));
				crudo.getMedia().add(referencia);
			}
		}
		return crudo;
	}

	private String textoDe(JsonNode mensaje, String tipo) {
		String cuerpo = mensaje.path("text").path("body").asText(null);
		if (cuerpo != null && !cuerpo.isBlank()) {
			return cuerpo;
		}
		String descripcion = mensaje.path(tipo).path("caption").asText(null);
		return descripcion == null || descripcion.isBlank() ? null : descripcion;
	}

	private Instant instanteDe(String marca) {
		if (marca == null || marca.isBlank()) {
			return Instant.now();
		}
		try {
			return Instant.ofEpochSecond(Long.parseLong(marca.trim()));
		}
		catch (NumberFormatException e) {
			return Instant.now();
		}
	}

	private void exigirFirma(LineaWhatsapp linea, String firma, byte[] cuerpo) {
		String secreto = ResolvedorSecreto.resolver(linea.getReferenciaSecretoAplicacion()).orElse(null);
		if (secreto == null) {
			if (!propiedades.isExigirFirma()) {
				log.warn("La linea {} no resuelve su secreto de aplicacion y la verificacion de firma esta"
						+ " desactivada: el webhook queda abierto", linea.getNumeroTelefono());
				return;
			}
			rechazar(linea, "No se pudo resolver el secreto de aplicacion "
					+ linea.getReferenciaSecretoAplicacion());
		}
		if (firma == null || firma.isBlank()) {
			rechazar(linea, "El evento llego sin la cabecera " + CABECERA_FIRMA);
		}
		String recibida = firma.trim();
		if (!recibida.startsWith(PREFIJO_FIRMA)) {
			rechazar(linea, "La cabecera " + CABECERA_FIRMA + " no arranca con " + PREFIJO_FIRMA);
		}
		String esperada = Hash.hmacSha256(secreto, cuerpo);
		if (!Hash.sonIguales(esperada, recibida.substring(PREFIJO_FIRMA.length()))) {
			rechazar(linea, "La firma del evento no coincide con el secreto de la aplicacion");
		}
	}

	private void rechazar(LineaWhatsapp linea, String motivo) {
		auditoriaService.registrarFallo(linea.getTenant().getId(), AccionAuditoria.WEBHOOK_WHATSAPP_RECHAZADO,
				LineaWhatsappService.ENTIDAD, linea.getId(),
				Map.of("linea", linea.getNumeroTelefono(), "motivo", motivo));
		log.warn("Webhook rechazado para la linea {}: {}", linea.getNumeroTelefono(), motivo);
		throw new NoAutorizadoException("El evento de WhatsApp no se pudo verificar: " + motivo);
	}

	private LineaWhatsapp buscarPorRuta(String rutaWebhook) {
		return lineaWhatsappRepository.buscarPorRutaWebhook(rutaWebhook == null ? "" : rutaWebhook.trim())
				.orElseThrow(() -> EntidadNoEncontradaException.de(LineaWhatsappService.ENTIDAD, rutaWebhook));
	}

	public String firmar(String secreto, String cuerpo) {
		return PREFIJO_FIRMA + Hash.hmacSha256(secreto, cuerpo.getBytes(StandardCharsets.UTF_8));
	}
}
