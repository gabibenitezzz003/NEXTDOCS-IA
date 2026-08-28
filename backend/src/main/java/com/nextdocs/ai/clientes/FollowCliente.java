package com.nextdocs.ai.clientes;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.entidades.ConfiguracionConector;
import com.nextdocs.ai.enumeraciones.TipoAutenticacionConector;
import com.nextdocs.ai.exceptions.ConectorNoDisponibleException;
import com.nextdocs.ai.utiles.ResolvedorSecreto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FollowCliente {

	public static final String CODIGO = "FOLLOW";

	private static final Logger log = LoggerFactory.getLogger(FollowCliente.class);

	private static final String CABECERA_POR_DEFECTO = "apiKey";

	private final HttpClient clienteHttp;

	private final ObjectMapper objectMapper;

	public FollowCliente(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.clienteHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	}

	public JsonNode buscar(ConfiguracionConector configuracion, String ruta, String parametroBusqueda,
			String termino) {
		String destino = normalizar(configuracion.getUrlBase()) + ruta + "?" + parametroBusqueda + "="
				+ URLEncoder.encode(termino, StandardCharsets.UTF_8);
		try {
			HttpRequest.Builder constructor = HttpRequest.newBuilder(URI.create(destino))
					.timeout(Duration.ofMillis(configuracion.getTiempoEsperaMilisegundos()))
					.header("Accept", "application/json")
					.GET();
			agregarAutenticacion(constructor, configuracion);
			HttpResponse<String> respuesta = clienteHttp.send(constructor.build(),
					HttpResponse.BodyHandlers.ofString());
			return interpretar(configuracion, respuesta);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ConectorNoDisponibleException(configuracion.getCodigo(),
					"La consulta al conector fue interrumpida", e);
		} catch (ConectorNoDisponibleException e) {
			throw e;
		} catch (Exception e) {
			throw new ConectorNoDisponibleException(configuracion.getCodigo(),
					"No se pudo contactar al conector: " + e.getClass().getSimpleName() + " " + e.getMessage(), e);
		}
	}

	private void agregarAutenticacion(HttpRequest.Builder constructor, ConfiguracionConector configuracion) {
		if (configuracion.getTipoAutenticacion() == null
				|| configuracion.getTipoAutenticacion() == TipoAutenticacionConector.NINGUNA) {
			return;
		}
		String secreto = ResolvedorSecreto.resolver(configuracion.getReferenciaSecreto())
				.orElseThrow(() -> new ConectorNoDisponibleException(configuracion.getCodigo(),
						"No hay credencial resoluble para el conector", false));
		if (configuracion.getTipoAutenticacion() == TipoAutenticacionConector.BEARER) {
			constructor.header("Authorization", "Bearer " + secreto);
			return;
		}
		String cabecera = configuracion.getNombreCabeceraClave() == null
				|| configuracion.getNombreCabeceraClave().isBlank() ? CABECERA_POR_DEFECTO
						: configuracion.getNombreCabeceraClave();
		constructor.header(cabecera, secreto);
	}

	private JsonNode interpretar(ConfiguracionConector configuracion, HttpResponse<String> respuesta)
			throws Exception {
		int codigo = respuesta.statusCode();
		if (codigo >= 200 && codigo < 300) {
			return objectMapper.readTree(respuesta.body());
		}
		log.warn("El conector {} respondio {}", configuracion.getCodigo(), codigo);
		throw new ConectorNoDisponibleException(configuracion.getCodigo(),
				"El conector respondio codigo HTTP " + codigo, false);
	}

	private String normalizar(String urlBase) {
		if (urlBase == null || urlBase.isBlank()) {
			throw new ConectorNoDisponibleException(CODIGO, "El conector no tiene urlBase configurada", false);
		}
		return urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
	}
}
