package com.nextdocs.ai.clientes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.exceptions.ProveedorNoDisponibleException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekCliente {

	public static final String CODIGO_CUOTA_EXCEDIDA = "CUOTA_EXCEDIDA";

	public static final String CODIGO_CREDENCIAL_INVALIDA = "CREDENCIAL_INVALIDA";

	private static final Logger log = LoggerFactory.getLogger(DeepSeekCliente.class);

	private final HttpClient clienteHttp;

	private final ObjectMapper objectMapper;

	private final String urlBasePorDefecto;

	private final int tiempoEsperaSegundos;

	public DeepSeekCliente(ObjectMapper objectMapper,
			@Value("${nextdocs.deepseek.urlBase:https://api.deepseek.com/v1}") String urlBasePorDefecto,
			@Value("${nextdocs.deepseek.tiempoEsperaSegundos:120}") int tiempoEsperaSegundos) {
		this.objectMapper = objectMapper;
		this.urlBasePorDefecto = urlBasePorDefecto;
		this.tiempoEsperaSegundos = tiempoEsperaSegundos;
		this.clienteHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
	}

	public JsonNode completar(String urlBase, String claveApi, String cuerpo) {
		String destino = normalizar(urlBase) + "/chat/completions";
		try {
			HttpRequest peticion = HttpRequest.newBuilder(URI.create(destino))
					.timeout(Duration.ofSeconds(tiempoEsperaSegundos))
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + claveApi)
					.POST(HttpRequest.BodyPublishers.ofString(cuerpo))
					.build();
			HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());
			return interpretar(respuesta);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ProveedorNoDisponibleException("La llamada a DeepSeek fue interrumpida", true, e);
		} catch (ProveedorNoDisponibleException e) {
			throw e;
		} catch (Exception e) {
			throw new ProveedorNoDisponibleException("No se pudo contactar a DeepSeek: " + e.getMessage(), true, e);
		}
	}

	private JsonNode interpretar(HttpResponse<String> respuesta) throws Exception {
		int codigo = respuesta.statusCode();
		if (codigo >= 200 && codigo < 300) {
			return objectMapper.readTree(respuesta.body());
		}
		String detalle = extraerMensaje(respuesta.body());
		log.warn("DeepSeek respondio {} : {}", codigo, detalle);
		if (codigo == 429) {
			throw new ProveedorNoDisponibleException(CODIGO_CUOTA_EXCEDIDA + ": " + detalle, true);
		}
		if (codigo == 401 || codigo == 403) {
			throw new ProveedorNoDisponibleException(CODIGO_CREDENCIAL_INVALIDA + ": " + detalle, false);
		}
		if (codigo >= 500) {
			throw new ProveedorNoDisponibleException("DeepSeek no disponible (" + codigo + "): " + detalle, true);
		}
		throw new ProveedorNoDisponibleException("DeepSeek rechazo la peticion (" + codigo + "): " + detalle, false);
	}

	private String extraerMensaje(String cuerpo) {
		if (cuerpo == null || cuerpo.isBlank()) {
			return "sin detalle";
		}
		try {
			JsonNode error = objectMapper.readTree(cuerpo).path("error");
			if (error.hasNonNull("message")) {
				return error.get("message").asText();
			}
		} catch (Exception e) {
			log.debug("La respuesta de error de DeepSeek no es JSON");
		}
		return cuerpo.length() > 300 ? cuerpo.substring(0, 300) : cuerpo;
	}

	private String normalizar(String urlBase) {
		String efectiva = urlBase == null || urlBase.isBlank() ? urlBasePorDefecto : urlBase;
		return efectiva.endsWith("/") ? efectiva.substring(0, efectiva.length() - 1) : efectiva;
	}
}
