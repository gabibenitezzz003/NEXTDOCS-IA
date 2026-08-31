package com.nextdocs.ai.servicios;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.nextdocs.ai.config.PropiedadesFederacion;
import com.nextdocs.ai.entidades.ProveedorIdentidad;
import com.nextdocs.ai.exceptions.NoAutorizadoException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VerificadorTokenIdpService {

	private static final Logger log = LoggerFactory.getLogger(VerificadorTokenIdpService.class);

	private final PropiedadesFederacion propiedades;

	private final HttpClient cliente;

	private final Map<String, ClavesProveedor> cache = new ConcurrentHashMap<>();

	public VerificadorTokenIdpService(PropiedadesFederacion propiedades) {
		this.propiedades = propiedades;
		this.cliente = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(propiedades.getTiempoEsperaMilisegundos())).build();
	}

	public Claims verificar(ProveedorIdentidad proveedor, String token) {
		if (token == null || token.isBlank()) {
			throw new NoAutorizadoException("El token del proveedor de identidad es obligatorio");
		}
		Claims claims;
		try {
			claims = Jwts.parser().keyLocator(new LocalizadorDeClave(proveedor))
					.requireIssuer(proveedor.getEmisor())
					.clockSkewSeconds(propiedades.getToleranciaRelojSegundos()).build()
					.parseSignedClaims(token.trim()).getPayload();
		}
		catch (NoAutorizadoException e) {
			throw e;
		}
		catch (Exception e) {
			log.warn("Token rechazado para el proveedor {}: {}", proveedor.getCodigo(), e.getMessage());
			throw new NoAutorizadoException("El token del proveedor de identidad no es valido");
		}
		exigirAudiencia(proveedor, claims);
		return claims;
	}

	private void exigirAudiencia(ProveedorIdentidad proveedor, Claims claims) {
		String esperada = proveedor.getAudiencia();
		if (esperada == null || esperada.isBlank()) {
			return;
		}
		if (claims.getAudience() != null && claims.getAudience().contains(esperada)) {
			return;
		}
		if (esperada.equals(claims.get("azp", String.class))) {
			return;
		}
		throw new NoAutorizadoException("El token no fue emitido para " + esperada);
	}

	private Key clavePara(ProveedorIdentidad proveedor, String identificadorClave) {
		ClavesProveedor claves = cache.get(proveedor.getId());
		if (claves == null || claves.vencidas(propiedades.getSegundosCacheJwks())
				|| !claves.contiene(identificadorClave)) {
			claves = descargar(proveedor);
			cache.put(proveedor.getId(), claves);
		}
		Key clave = claves.buscar(identificadorClave);
		if (clave == null) {
			throw new NoAutorizadoException(
					"El proveedor " + proveedor.getCodigo() + " no publica la clave " + identificadorClave);
		}
		return clave;
	}

	private ClavesProveedor descargar(ProveedorIdentidad proveedor) {
		try {
			HttpRequest peticion = HttpRequest.newBuilder(URI.create(proveedor.getUrlJwks()))
					.timeout(Duration.ofMillis(propiedades.getTiempoEsperaMilisegundos())).GET().build();
			HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
			if (respuesta.statusCode() >= 300) {
				throw new IllegalStateException("El JWKS respondio " + respuesta.statusCode());
			}
			JwkSet conjunto = Jwks.setParser().build().parse(respuesta.body());
			Map<String, Key> claves = new LinkedHashMap<>();
			for (Jwk<?> jwk : conjunto.getKeys()) {
				if (jwk.getId() != null && jwk.toKey() instanceof Key clave) {
					claves.put(jwk.getId(), clave);
				}
			}
			log.info("JWKS del proveedor {} actualizado con {} clave(s)", proveedor.getCodigo(), claves.size());
			return new ClavesProveedor(claves, Instant.now());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new NoAutorizadoException("No se pudo leer el JWKS de " + proveedor.getCodigo());
		}
		catch (Exception e) {
			String causa = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
			log.error("No se pudo leer el JWKS de {} en {}: {}", proveedor.getCodigo(), proveedor.getUrlJwks(),
					causa);
			throw new NoAutorizadoException("No se pudo leer el JWKS de " + proveedor.getCodigo() + " en "
					+ proveedor.getUrlJwks() + ": " + causa
					+ ". La urlJwks tiene que ser alcanzable desde el backend, que no siempre resuelve"
					+ " el mismo host que el navegador");
		}
	}

	public void olvidar(String proveedorId) {
		cache.remove(proveedorId);
	}

	private final class LocalizadorDeClave extends LocatorAdapter<Key> {

		private final ProveedorIdentidad proveedor;

		private LocalizadorDeClave(ProveedorIdentidad proveedor) {
			this.proveedor = proveedor;
		}

		@Override
		protected Key locate(JwsHeader cabecera) {
			return clavePara(proveedor, cabecera.getKeyId());
		}
	}

	private record ClavesProveedor(Map<String, Key> claves, Instant descargadas) {

		private boolean vencidas(int segundos) {
			return descargadas.plusSeconds(segundos).isBefore(Instant.now());
		}

		private boolean contiene(String identificador) {
			return identificador != null && claves.containsKey(identificador);
		}

		private Key buscar(String identificador) {
			if (identificador != null) {
				return claves.get(identificador);
			}
			List<Key> unica = List.copyOf(claves.values());
			return unica.size() == 1 ? unica.get(0) : null;
		}
	}
}
