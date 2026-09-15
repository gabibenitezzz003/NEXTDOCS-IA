package com.nextdocs.ai.servicios;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.config.PropiedadesFederacion;
import com.nextdocs.ai.config.PropiedadesSeguridad;
import com.nextdocs.ai.entidades.ProveedorIdentidad;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.EstadoTenant;
import com.nextdocs.ai.exceptions.NoAutorizadoException;
import com.nextdocs.ai.exceptions.ProhibidoException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.CodigoEmbedModel;
import com.nextdocs.ai.modelos.IntercambioFederadoReqModel;
import com.nextdocs.ai.modelos.ProveedorOauthPublicoModel;
import com.nextdocs.ai.repositorios.ProveedorIdentidadRepository;
import com.nextdocs.ai.repositorios.TenantRepository;

import io.jsonwebtoken.Claims;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OauthLoginService {

	private static final Logger log = LoggerFactory.getLogger(OauthLoginService.class);

	public static final String RUTA_CALLBACK = "/api/v1/federacion/oauth/callback";

	private static final long VIGENCIA_ESTADO_SEGUNDOS = 300;

	private static final SecureRandom AZAR = new SecureRandom();

	private final ProveedorIdentidadRepository proveedorIdentidadRepository;

	private final TenantRepository tenantRepository;

	private final FederacionIdentidadService federacionIdentidadService;

	private final VerificadorTokenIdpService verificadorTokenIdpService;

	private final PropiedadesFederacion propiedades;

	private final PropiedadesSeguridad seguridad;

	private final ObjectMapper mapeador;

	private final HttpClient http;

	public OauthLoginService(ProveedorIdentidadRepository proveedorIdentidadRepository,
			TenantRepository tenantRepository, FederacionIdentidadService federacionIdentidadService,
			VerificadorTokenIdpService verificadorTokenIdpService, PropiedadesFederacion propiedades,
			PropiedadesSeguridad seguridad, ObjectMapper mapeador) {
		this.proveedorIdentidadRepository = proveedorIdentidadRepository;
		this.tenantRepository = tenantRepository;
		this.federacionIdentidadService = federacionIdentidadService;
		this.verificadorTokenIdpService = verificadorTokenIdpService;
		this.propiedades = propiedades;
		this.seguridad = seguridad;
		this.mapeador = mapeador;
		this.http = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(propiedades.getTiempoEsperaMilisegundos())).build();
	}

	@Transactional(readOnly = true)
	public List<ProveedorOauthPublicoModel> listarPublicos(String codigoTenant) {
		Tenant tenant = tenantRepository.findByCodigoAndBajaIsNull(codigoTenant)
				.filter(t -> t.getEstado() == EstadoTenant.ACTIVO).orElse(null);
		if (tenant == null) {
			return List.of();
		}
		return proveedorIdentidadRepository.listarPorTenant(tenant.getId()).stream()
				.filter(ProveedorIdentidad::isActivo).filter(ProveedorIdentidadService::oauthCompleto)
				.map(proveedor -> {
					ProveedorOauthPublicoModel modelo = new ProveedorOauthPublicoModel();
					modelo.setCodigo(proveedor.getCodigo());
					modelo.setNombre(proveedor.getNombre());
					return modelo;
				}).toList();
	}

	@Transactional(readOnly = true)
	public String iniciar(String codigoTenant, String codigoProveedor, String retorno) {
		ProveedorIdentidad proveedor = proveedorOauth(codigoTenant, codigoProveedor);
		String destino = validarRetorno(proveedor, retorno);
		String nonce = aleatorio();
		String estado = firmarEstado(codigoTenant, proveedor.getCodigo(), nonce, destino);
		String alcances = proveedor.getAlcances() == null || proveedor.getAlcances().isBlank()
				? "openid email profile"
				: proveedor.getAlcances().trim();
		return proveedor.getUrlAutorizacion() + "?response_type=code"
				+ "&client_id=" + codificar(proveedor.getClienteId())
				+ "&redirect_uri=" + codificar(urlCallback())
				+ "&scope=" + codificar(alcances)
				+ "&state=" + codificar(estado)
				+ "&nonce=" + codificar(nonce);
	}

	public String resolverCallback(String codigo, String estado, String errorIdp) {
		EstadoOauth datos;
		try {
			datos = leerEstado(estado);
		}
		catch (RuntimeException e) {
			return urlError("El estado de la federacion no es valido o vencio");
		}
		String portal = datos.retorno == null ? urlPortal() : datos.retorno;
		if (errorIdp != null && !errorIdp.isBlank()) {
			return urlErrorPortal(portal, "El proveedor rechazo el ingreso: " + errorIdp);
		}
		try {
			ProveedorIdentidad proveedor = proveedorOauth(datos.tenant, datos.proveedor);
			String idToken = intercambiarCodigo(proveedor, codigo);
			Claims claims = verificadorTokenIdpService.verificar(proveedor, idToken);
			String nonce = claims.get("nonce", String.class);
			if (!datos.nonce.equals(nonce)) {
				throw new NoAutorizadoException("El token federado no corresponde a esta solicitud");
			}
			IntercambioFederadoReqModel intercambio = new IntercambioFederadoReqModel();
			intercambio.setCodigoTenant(datos.tenant);
			intercambio.setProveedor(datos.proveedor);
			intercambio.setToken(idToken);
			CodigoEmbedModel resultado = federacionIdentidadService.intercambiar(intercambio);
			return portal + "/ingresar?codigo=" + codificar(resultado.getCodigo());
		}
		catch (RuntimeException e) {
			log.warn("Fallo el callback OAuth para el tenant {}: {}", datos.tenant, e.getMessage());
			return urlErrorPortal(portal, e.getMessage());
		}
	}

	private ProveedorIdentidad proveedorOauth(String codigoTenant, String codigoProveedor) {
		ProveedorIdentidad proveedor = proveedorIdentidadRepository
				.buscarPorCodigoTenantYCodigo(codigoTenant, codigoProveedor)
				.filter(ProveedorIdentidad::isActivo)
				.orElseThrow(() -> new NoAutorizadoException(
						"No hay un proveedor de identidad activo llamado " + codigoProveedor));
		if (!ProveedorIdentidadService.oauthCompleto(proveedor)) {
			throw new ProhibidoException("El proveedor " + codigoProveedor
					+ " no tiene configurado el login social");
		}
		return proveedor;
	}

	private String intercambiarCodigo(ProveedorIdentidad proveedor, String codigo) {
		String cuerpo = "grant_type=authorization_code"
				+ "&code=" + codificar(codigo)
				+ "&redirect_uri=" + codificar(urlCallback())
				+ "&client_id=" + codificar(proveedor.getClienteId())
				+ "&client_secret=" + codificar(proveedor.getClienteSecreto());
		HttpRequest peticion = HttpRequest.newBuilder(URI.create(proveedor.getUrlToken()))
				.timeout(Duration.ofMillis(propiedades.getTiempoEsperaMilisegundos()))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build();
		try {
			HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
			if (respuesta.statusCode() != 200) {
				throw new NoAutorizadoException(
						"El proveedor rechazo el canje del codigo: HTTP " + respuesta.statusCode());
			}
			JsonNode json = mapeador.readTree(respuesta.body());
			JsonNode idToken = json.get("id_token");
			if (idToken == null || idToken.asText().isBlank()) {
				throw new NoAutorizadoException("El proveedor no devolvio un id_token");
			}
			return idToken.asText();
		}
		catch (NoAutorizadoException e) {
			throw e;
		}
		catch (Exception e) {
			throw new NoAutorizadoException(
					"No se pudo canjear el codigo con el proveedor: " + e.getMessage());
		}
	}

	private String firmarEstado(String codigoTenant, String proveedor, String nonce, String retorno) {
		StringBuilder json = new StringBuilder("{\"t\":\"").append(escapar(codigoTenant))
				.append("\",\"p\":\"").append(escapar(proveedor)).append("\",\"n\":\"").append(nonce)
				.append("\",\"e\":").append(Instant.now().getEpochSecond() + VIGENCIA_ESTADO_SEGUNDOS);
		if (retorno != null) {
			json.append(",\"r\":\"").append(escapar(retorno)).append("\"");
		}
		String payload = json.append("}").toString();
		String cuerpo = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
		return cuerpo + "." + firma(cuerpo);
	}

	private EstadoOauth leerEstado(String estado) {
		if (estado == null || estado.isBlank() || !estado.contains(".")) {
			throw new NoAutorizadoException("El estado de la federacion no es valido");
		}
		String cuerpo = estado.substring(0, estado.lastIndexOf('.'));
		String firmaRecibida = estado.substring(estado.lastIndexOf('.') + 1);
		if (!firma(cuerpo).equals(firmaRecibida)) {
			throw new NoAutorizadoException("El estado de la federacion fue alterado");
		}
		try {
			JsonNode json = mapeador
					.readTree(Base64.getUrlDecoder().decode(cuerpo));
			long vence = json.get("e").asLong();
			if (Instant.now().getEpochSecond() > vence) {
				throw new NoAutorizadoException("El estado de la federacion vencio");
			}
			EstadoOauth datos = new EstadoOauth();
			datos.tenant = json.get("t").asText();
			datos.proveedor = json.get("p").asText();
			datos.nonce = json.get("n").asText();
			datos.retorno = json.hasNonNull("r") ? json.get("r").asText() : null;
			return datos;
		}
		catch (NoAutorizadoException e) {
			throw e;
		}
		catch (Exception e) {
			throw new NoAutorizadoException("El estado de la federacion no es valido");
		}
	}

	private String firma(String cuerpo) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(seguridad.getJwtSecreto().getBytes(StandardCharsets.UTF_8),
					"HmacSHA256"));
			return Base64.getUrlEncoder().withoutPadding()
					.encodeToString(mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception e) {
			throw new IllegalStateException("No se pudo firmar el estado OAuth", e);
		}
	}

	private String validarRetorno(ProveedorIdentidad proveedor, String retorno) {
		if (retorno == null || retorno.isBlank()) {
			return null;
		}
		List<String> permitidos = proveedor.getOrigenesEmbedPermitidos() == null ? List.of()
				: Arrays.stream(proveedor.getOrigenesEmbedPermitidos().split(",")).map(String::trim)
						.filter(texto -> !texto.isEmpty()).toList();
		String origen;
		try {
			URI uri = URI.create(retorno.trim());
			if (uri.getScheme() == null || uri.getHost() == null) {
				throw new IllegalArgumentException("retorno incompleto");
			}
			origen = uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT)
					+ (uri.getPort() > 0 ? ":" + uri.getPort() : "");
		}
		catch (Exception e) {
			throw new ProhibidoException("El retorno no es una direccion absoluta valida");
		}
		if (permitidos.stream().noneMatch(permitido -> permitido.equalsIgnoreCase(origen))) {
			throw new ProhibidoException("El origen " + origen + " no esta permitido para el proveedor "
					+ proveedor.getCodigo());
		}
		return origen;
	}

	private String urlCallback() {
		return propiedades.getUrlBaseApi() + RUTA_CALLBACK;
	}

	private String urlPortal() {
		return propiedades.getUrlBasePortal();
	}

	private String urlError(String mensaje) {
		return urlErrorPortal(urlPortal(), mensaje);
	}

	private String urlErrorPortal(String portal, String mensaje) {
		return portal + "/ingresar?errorFederado=" + codificar(mensaje);
	}

	private String codificar(String valor) {
		return URLEncoder.encode(valor, StandardCharsets.UTF_8);
	}

	private String aleatorio() {
		byte[] bytes = new byte[24];
		AZAR.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String escapar(String texto) {
		return texto.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static class EstadoOauth {
		String tenant;
		String proveedor;
		String nonce;
		String retorno;
	}
}
