package com.nextdocs.ai.servicios;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.config.PropiedadesWhatsapp;
import com.nextdocs.ai.entidades.LineaWhatsapp;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.MediaDescargadaModel;
import com.nextdocs.ai.utiles.ResolvedorSecreto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ClienteWhatsappService {

	public static final String CODIGO_MEDIA_NO_DISPONIBLE = "MEDIA_NO_DISPONIBLE";

	public static final String CODIGO_MEDIA_EXCEDIDA = "MEDIA_EXCEDE_TAMANO";

	private static final Logger log = LoggerFactory.getLogger(ClienteWhatsappService.class);

	private final PropiedadesWhatsapp propiedades;

	private final ObjectMapper objectMapper;

	private final HttpClient cliente;

	public ClienteWhatsappService(PropiedadesWhatsapp propiedades, ObjectMapper objectMapper) {
		this.propiedades = propiedades;
		this.objectMapper = objectMapper;
		this.cliente = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(propiedades.getTiempoEsperaMilisegundos())).build();
	}

	public MediaDescargadaModel descargar(LineaWhatsapp linea, String identificadorMedia, String nombreDeclarado) {
		MediaDescargadaModel media = new MediaDescargadaModel();
		media.setIdentificadorMedia(identificadorMedia);
		try {
			String token = tokenDe(linea);
			JsonNode metadatos = pedirJson(urlDe(identificadorMedia), token);
			long tamano = metadatos.path("file_size").asLong(0);
			String tipoMime = limpiarTipoMime(metadatos.path("mime_type").asText(null));
			media.setTipoMime(tipoMime);
			media.setTamanoDeclarado(tamano);
			media.setNombreArchivo(nombreDe(nombreDeclarado, identificadorMedia, tipoMime));

			if (tamano > propiedades.getTamanoMaximoMediaBytes()) {
				media.setCodigoRechazo(CODIGO_MEDIA_EXCEDIDA);
				media.setMotivo("La media pesa " + tamano + " bytes y el canal acepta hasta "
						+ propiedades.getTamanoMaximoMediaBytes());
				return media;
			}
			String urlBinaria = metadatos.path("url").asText(null);
			if (urlBinaria == null || urlBinaria.isBlank()) {
				media.setCodigoRechazo(CODIGO_MEDIA_NO_DISPONIBLE);
				media.setMotivo("La API no devolvio una URL de descarga para la media " + identificadorMedia);
				return media;
			}
			media.setContenido(pedirBinario(urlBinaria, token));
			return media;
		}
		catch (Exception e) {
			log.warn("No se pudo descargar la media {} de la linea {}: {}", identificadorMedia,
					linea.getNumeroTelefono(), e.getMessage());
			media.setCodigoRechazo(CODIGO_MEDIA_NO_DISPONIBLE);
			media.setMotivo(detalleDe(e));
			return media;
		}
	}

	public String enviarTexto(LineaWhatsapp linea, String numeroDestino, String cuerpo) {
		Map<String, Object> carga = new LinkedHashMap<>();
		carga.put("messaging_product", "whatsapp");
		carga.put("recipient_type", "individual");
		carga.put("to", numeroDestino);
		carga.put("type", "text");
		carga.put("text", Map.of("preview_url", false, "body", cuerpo));
		return despachar(linea, carga);
	}

	public String enviarPlantilla(LineaWhatsapp linea, String numeroDestino, String nombrePlantilla, String idioma,
			List<String> parametros) {
		Map<String, Object> plantilla = new LinkedHashMap<>();
		plantilla.put("name", nombrePlantilla);
		plantilla.put("language", Map.of("code", idioma == null || idioma.isBlank() ? "es" : idioma));
		if (parametros != null && !parametros.isEmpty()) {
			plantilla.put("components", List.of(Map.of("type", "body", "parameters",
					parametros.stream().map(valor -> Map.of("type", "text", "text", valor)).toList())));
		}
		Map<String, Object> carga = new LinkedHashMap<>();
		carga.put("messaging_product", "whatsapp");
		carga.put("recipient_type", "individual");
		carga.put("to", numeroDestino);
		carga.put("type", "template");
		carga.put("template", plantilla);
		return despachar(linea, carga);
	}

	public void verificar(LineaWhatsapp linea) {
		try {
			JsonNode respuesta = pedirJson(urlDe(linea.getIdentificadorNumero()), tokenDe(linea));
			String numero = respuesta.path("display_phone_number").asText(null);
			if (numero != null && !numero.isBlank()) {
				log.info("La linea {} responde como {}", linea.getNombre(), numero);
			}
		}
		catch (Exception e) {
			throw new ValidacionException("No se pudo consultar la linea " + linea.getNumeroTelefono() + " en "
					+ propiedades.getUrlGraph() + ": " + detalleDe(e)
					+ ". Revisa el token de acceso y que el identificador del numero sea el de la Cloud API");
		}
	}

	private String despachar(LineaWhatsapp linea, Map<String, Object> carga) {
		try {
			String cuerpo = objectMapper.writeValueAsString(carga);
			HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlDe(linea.getIdentificadorNumero() + "/messages")))
					.timeout(Duration.ofMillis(propiedades.getTiempoEsperaMilisegundos()))
					.header("Authorization", "Bearer " + tokenDe(linea))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8)).build();
			HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
			if (respuesta.statusCode() >= 300) {
				throw new ValidacionException("La Cloud API respondio " + respuesta.statusCode() + ": "
						+ recortar(respuesta.body()));
			}
			return objectMapper.readTree(respuesta.body()).path("messages").path(0).path("id").asText(null);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ValidacionException("Se interrumpio el envio a la Cloud API");
		}
		catch (ValidacionException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ValidacionException("No se pudo enviar el mensaje por la Cloud API: " + detalleDe(e));
		}
	}

	private JsonNode pedirJson(String url, String token) throws Exception {
		HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofMillis(propiedades.getTiempoEsperaMilisegundos()))
				.header("Authorization", "Bearer " + token).GET().build();
		HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
		if (respuesta.statusCode() >= 300) {
			throw new IllegalStateException("respondio " + respuesta.statusCode() + ": " + recortar(respuesta.body()));
		}
		return objectMapper.readTree(respuesta.body());
	}

	private byte[] pedirBinario(String url, String token) throws Exception {
		HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofMillis(propiedades.getTiempoEsperaMilisegundos()))
				.header("Authorization", "Bearer " + token).GET().build();
		HttpResponse<InputStream> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofInputStream());
		if (respuesta.statusCode() >= 300) {
			throw new IllegalStateException("la descarga respondio " + respuesta.statusCode());
		}
		try (InputStream flujo = respuesta.body()) {
			byte[] contenido = flujo.readNBytes((int) propiedades.getTamanoMaximoMediaBytes() + 1);
			if (contenido.length > propiedades.getTamanoMaximoMediaBytes()) {
				throw new IllegalStateException("el contenido supera el tope de "
						+ propiedades.getTamanoMaximoMediaBytes() + " bytes declarado por el canal");
			}
			return contenido;
		}
	}

	private String urlDe(String recurso) {
		StringBuilder url = new StringBuilder(propiedades.getUrlGraph());
		if (url.charAt(url.length() - 1) != '/') {
			url.append('/');
		}
		String version = propiedades.getVersionGraph();
		if (version != null && !version.isBlank()) {
			url.append(version).append('/');
		}
		return url.append(recurso).toString();
	}

	private String tokenDe(LineaWhatsapp linea) {
		return ResolvedorSecreto.resolver(linea.getReferenciaTokenAcceso())
				.orElseThrow(() -> new ValidacionException("No se pudo resolver el token de acceso de la linea "
						+ linea.getNumeroTelefono() + " con la referencia " + linea.getReferenciaTokenAcceso()));
	}

	private String nombreDe(String nombreDeclarado, String identificadorMedia, String tipoMime) {
		if (nombreDeclarado != null && !nombreDeclarado.isBlank()) {
			return nombreDeclarado.trim();
		}
		return "whatsapp-" + identificadorMedia + "." + extensionDe(tipoMime);
	}

	private String extensionDe(String tipoMime) {
		if (tipoMime == null) {
			return "bin";
		}
		return switch (tipoMime) {
			case "application/pdf" -> "pdf";
			case "image/jpeg" -> "jpg";
			case "image/png" -> "png";
			case "image/webp" -> "webp";
			case "image/tiff" -> "tiff";
			default -> "bin";
		};
	}

	private String limpiarTipoMime(String tipoMime) {
		if (tipoMime == null) {
			return null;
		}
		int puntoYComa = tipoMime.indexOf(';');
		return (puntoYComa < 0 ? tipoMime : tipoMime.substring(0, puntoYComa)).trim();
	}

	private String detalleDe(Exception e) {
		return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
	}

	private String recortar(String valor) {
		if (valor == null) {
			return "";
		}
		return valor.length() <= 300 ? valor : valor.substring(0, 300);
	}
}
