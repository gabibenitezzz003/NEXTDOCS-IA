package com.nextdocs.ai.servicios;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.nextdocs.ai.entidades.ConfiguracionConector;
import com.nextdocs.ai.enumeraciones.TipoAutenticacionConector;
import com.nextdocs.ai.exceptions.ConectorNoDisponibleException;
import com.nextdocs.ai.repositorios.ConfiguracionConectorRepository;
import com.nextdocs.ai.utiles.ResolvedorSecreto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentalProxyService {

	public static final String CODIGO_CONECTOR = "MOTOR_DOCUMENTAL";

	private static final Logger log = LoggerFactory.getLogger(DocumentalProxyService.class);

	private static final Duration ESPERA = Duration.ofSeconds(30);

	private final ConfiguracionConectorRepository configuracionConectorRepository;

	private final ProvisionadorDocumentalService provisionadorDocumentalService;

	private final HttpClient clienteHttp;

	private final Map<String, Object> cerrojos = new ConcurrentHashMap<>();

	public DocumentalProxyService(ConfiguracionConectorRepository configuracionConectorRepository,
			ProvisionadorDocumentalService provisionadorDocumentalService) {
		this.configuracionConectorRepository = configuracionConectorRepository;
		this.provisionadorDocumentalService = provisionadorDocumentalService;
		this.clienteHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	}

	public boolean habilitado(String tenantId) {
		Optional<ConfiguracionConector> activo = buscarActivo(tenantId);
		if (activo.isPresent()) {
			provisionadorDocumentalService.asegurarSuscripcionEventos(tenantId);
			return true;
		}
		return recuperar(tenantId).isPresent();
	}

	public HttpResponse<byte[]> reenviar(String tenantId, String metodo, String subruta, String consulta,
			byte[] cuerpo, String tipoContenido, String claveIdempotencia) {
		ConfiguracionConector configuracion = buscarActivo(tenantId)
				.orElseGet(() -> recuperar(tenantId).orElseThrow(() -> new ConectorNoDisponibleException(
						CODIGO_CONECTOR, "El tenant no tiene configurado el motor documental", false)));
		provisionadorDocumentalService.asegurarSuscripcionEventos(tenantId);

		String destino = normalizar(configuracion.getUrlBase()) + "/api/v1" + subruta
				+ (consulta == null || consulta.isBlank() ? "" : "?" + consulta);

		HttpRequest.Builder constructor = HttpRequest.newBuilder(URI.create(destino)).timeout(ESPERA)
				.header("Accept", "application/json");

		if (tipoContenido != null && !tipoContenido.isBlank()) {
			constructor.header("Content-Type", tipoContenido);
		}
		if (claveIdempotencia != null && !claveIdempotencia.isBlank()) {
			constructor.header("Idempotency-Key", claveIdempotencia);
		}

		constructor.method(metodo,
				cuerpo == null || cuerpo.length == 0 ? HttpRequest.BodyPublishers.noBody()
						: HttpRequest.BodyPublishers.ofByteArray(cuerpo));

		agregarAutenticacion(constructor, configuracion);

		try {
			return clienteHttp.send(constructor.build(), HttpResponse.BodyHandlers.ofByteArray());
		} catch (IOException e) {
			log.warn("fallo contactando motor documental: {}", e.getMessage());
			throw new ConectorNoDisponibleException(CODIGO_CONECTOR,
					"No se pudo contactar al motor documental", true);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ConectorNoDisponibleException(CODIGO_CONECTOR,
					"La llamada al motor documental fue interrumpida", true);
		}
	}

	private Optional<ConfiguracionConector> buscarActivo(String tenantId) {
		return configuracionConectorRepository.buscarPorCodigo(tenantId, CODIGO_CONECTOR)
				.filter(ConfiguracionConector::isActivo);
	}

	private Optional<ConfiguracionConector> recuperar(String tenantId) {
		Object cerrojo = cerrojos.computeIfAbsent(tenantId, clave -> new Object());
		synchronized (cerrojo) {
			Optional<ConfiguracionConector> activo = buscarActivo(tenantId);
			if (activo.isPresent()) {
				return activo;
			}
			provisionadorDocumentalService.provisionar(tenantId);
			return buscarActivo(tenantId);
		}
	}

	private void agregarAutenticacion(HttpRequest.Builder constructor, ConfiguracionConector configuracion) {
		if (configuracion.getTipoAutenticacion() == null
				|| configuracion.getTipoAutenticacion() == TipoAutenticacionConector.NINGUNA) {
			return;
		}
		String secreto = ResolvedorSecreto.resolver(configuracion.getReferenciaSecreto())
				.orElseThrow(() -> new ConectorNoDisponibleException(configuracion.getCodigo(),
						"No hay credencial resoluble para el motor documental", false));
		if (configuracion.getTipoAutenticacion() == TipoAutenticacionConector.BEARER) {
			constructor.header("Authorization", "Bearer " + secreto);
			return;
		}
		String cabecera = configuracion.getNombreCabeceraClave() == null
				|| configuracion.getNombreCabeceraClave().isBlank() ? "X-Clave-Api"
						: configuracion.getNombreCabeceraClave();
		constructor.header(cabecera, secreto);
	}

	private String normalizar(String urlBase) {
		String limpia = urlBase == null ? "" : urlBase.trim();
		return limpia.endsWith("/") ? limpia.substring(0, limpia.length() - 1) : limpia;
	}
}
