package com.nextdocs.ai.servicios;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.config.PropiedadesDocumental;
import com.nextdocs.ai.entidades.ConfiguracionConector;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.TipoAutenticacionConector;
import com.nextdocs.ai.repositorios.ConfiguracionConectorRepository;
import com.nextdocs.ai.repositorios.TenantRepository;
import com.nextdocs.ai.utiles.ResolvedorSecreto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProvisionadorDocumentalService {

	private static final Logger log = LoggerFactory.getLogger(ProvisionadorDocumentalService.class);

	private static final String ENTIDAD = "ConfiguracionConector";

	private static final String NOMBRE_CONECTOR = "Motor documental";

	private final PropiedadesDocumental propiedades;

	private final ConfiguracionConectorRepository configuracionConectorRepository;

	private final TenantRepository tenantRepository;

	private final AuditoriaService auditoriaService;

	private final ObjectMapper json;

	private final HttpClient clienteHttp;

	private final Map<String, Instant> ultimoFallo = new ConcurrentHashMap<>();

	public ProvisionadorDocumentalService(PropiedadesDocumental propiedades,
			ConfiguracionConectorRepository configuracionConectorRepository, TenantRepository tenantRepository,
			AuditoriaService auditoriaService, ObjectMapper json) {
		this.propiedades = propiedades;
		this.configuracionConectorRepository = configuracionConectorRepository;
		this.tenantRepository = tenantRepository;
		this.auditoriaService = auditoriaService;
		this.json = json;
		this.clienteHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean provisionar(String tenantId) {
		Optional<ConfiguracionConector> existente = configuracionConectorRepository
				.buscarPorCodigo(tenantId, DocumentalProxyService.CODIGO_CONECTOR);
		if (existente.filter(ConfiguracionConector::isActivo).isPresent()) {
			return true;
		}
		if (!propiedades.provisionHabilitada() || enEnfriamiento(tenantId)) {
			return false;
		}
		Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
		if (tenant == null) {
			log.warn("provision documental omitida: no existe el tenant {}", tenantId);
			return false;
		}
		return provisionar(tenant, existente.orElse(null));
	}

	@Transactional
	public boolean provisionar(Tenant tenant) {
		Optional<ConfiguracionConector> existente = configuracionConectorRepository
				.buscarPorCodigo(tenant.getId(), DocumentalProxyService.CODIGO_CONECTOR);
		if (existente.filter(ConfiguracionConector::isActivo).isPresent()) {
			return true;
		}
		if (!propiedades.provisionHabilitada() || enEnfriamiento(tenant.getId())) {
			return false;
		}
		return provisionar(tenant, existente.orElse(null));
	}

	private boolean provisionar(Tenant tenant, ConfiguracionConector existente) {
		String clave;
		try {
			clave = solicitarAltaRemota(tenant);
		} catch (Exception e) {
			ultimoFallo.put(tenant.getId(), Instant.now());
			log.warn("provision documental fallida para tenant {}: {}", tenant.getCodigo(), e.getMessage());
			registrarAuditoria(tenant.getId(), AccionAuditoria.CONECTOR_PROVISION_FALLIDA);
			return false;
		}
		ultimoFallo.remove(tenant.getId());
		guardarConector(tenant, existente, clave);
		registrarAuditoria(tenant.getId(), AccionAuditoria.CONECTOR_PROVISIONADO);
		log.info("motor documental provisionado para tenant {}", tenant.getCodigo());
		return true;
	}

	private String solicitarAltaRemota(Tenant tenant) throws IOException, InterruptedException {
		String destino = normalizar(propiedades.getUrlBase()) + "/api/v1/admin/inquilinos";
		String cuerpo = json.writeValueAsString(
				Map.of("inquilinoId", tenant.getId(), "nombre", tenant.getNombre()));
		HttpRequest pedido = HttpRequest.newBuilder(URI.create(destino))
				.timeout(Duration.ofMillis(propiedades.getTiempoEsperaMilisegundos()))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.header("Authorization", "Bearer " + propiedades.getClaveAdmin())
				.POST(HttpRequest.BodyPublishers.ofString(cuerpo))
				.build();
		HttpResponse<String> respuesta = clienteHttp.send(pedido, HttpResponse.BodyHandlers.ofString());
		if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
			throw new IOException("el motor respondio " + respuesta.statusCode());
		}
		JsonNode nodo = json.readTree(respuesta.body());
		String clave = nodo.path("clave").asText("");
		if (!clave.startsWith("ndk_")) {
			throw new IOException("el motor no devolvio una credencial valida");
		}
		return clave;
	}

	private void guardarConector(Tenant tenant, ConfiguracionConector existente, String clave) {
		ConfiguracionConector configuracion = existente != null ? existente : new ConfiguracionConector();
		configuracion.setTenant(tenant);
		configuracion.setCodigo(DocumentalProxyService.CODIGO_CONECTOR);
		configuracion.setNombre(NOMBRE_CONECTOR);
		configuracion.setUrlBase(normalizar(propiedades.getUrlBase()));
		configuracion.setTipoAutenticacion(TipoAutenticacionConector.BEARER);
		configuracion.setReferenciaSecreto(ResolvedorSecreto.PREFIJO_LITERAL + clave);
		configuracion.setTiempoEsperaMilisegundos(30000);
		configuracion.setIntentosMaximos(2);
		configuracion.setUmbralCircuitoAbierto(5);
		configuracion.setDuracionCircuitoAbiertoSegundos(60);
		configuracion.setActivo(true);
		configuracion.setBaja(null);
		if (configuracion.getAlta() == null) {
			configuracion.setAlta(Instant.now());
		}
		configuracionConectorRepository.save(configuracion);
	}

	private boolean enEnfriamiento(String tenantId) {
		Instant fallo = ultimoFallo.get(tenantId);
		return fallo != null
				&& Instant.now().isBefore(fallo.plusSeconds(propiedades.getEnfriamientoFalloSegundos()));
	}

	private void registrarAuditoria(String tenantId, AccionAuditoria accion) {
		try {
			auditoriaService.registrar(tenantId, accion, ENTIDAD, DocumentalProxyService.CODIGO_CONECTOR);
		} catch (Exception e) {
			log.debug("auditoria de provision omitida: {}", e.getMessage());
		}
	}

	private String normalizar(String urlBase) {
		String limpia = urlBase == null ? "" : urlBase.trim();
		return limpia.endsWith("/") ? limpia.substring(0, limpia.length() - 1) : limpia;
	}
}
