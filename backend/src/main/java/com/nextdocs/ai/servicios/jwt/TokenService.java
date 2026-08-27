package com.nextdocs.ai.servicios.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;

import com.nextdocs.ai.config.PropiedadesSeguridad;
import com.nextdocs.ai.entidades.Rol;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.TipoActor;
import com.nextdocs.ai.exceptions.NoAutorizadoException;
import com.nextdocs.ai.modelos.PrincipalNextDocs;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

@Service
public class TokenService {

	public static final String PREFIJO_BEARER = "Bearer ";

	private static final String CLAIM_TENANT = "tenantId";

	private static final String CLAIM_CODIGO_TENANT = "codigoTenant";

	private static final String CLAIM_PERMISOS = "permisos";

	private static final String CLAIM_TIPO = "tipo";

	private static final String TIPO_ACCESO = "acceso";

	private static final String TIPO_REFRESCO = "refresco";

	private final PropiedadesSeguridad propiedades;

	private final SecretKey clave;

	public TokenService(PropiedadesSeguridad propiedades) {
		this.propiedades = propiedades;
		this.clave = Keys.hmacShaKeyFor(propiedades.getJwtSecreto().getBytes(StandardCharsets.UTF_8));
	}

	public String generarAcceso(Usuario usuario) {
		Instant ahora = Instant.now();
		return Jwts.builder()
				.issuer(propiedades.getJwtEmisor())
				.subject(usuario.getId())
				.claim(CLAIM_TENANT, usuario.getTenant().getId())
				.claim(CLAIM_CODIGO_TENANT, usuario.getTenant().getCodigo())
				.claim(CLAIM_PERMISOS, List.copyOf(permisosDe(usuario)))
				.claim(CLAIM_TIPO, TIPO_ACCESO)
				.issuedAt(Date.from(ahora))
				.expiration(Date.from(ahora.plus(propiedades.getJwtDuracionMinutos(), ChronoUnit.MINUTES)))
				.signWith(clave)
				.compact();
	}

	public String generarRefresco(Usuario usuario) {
		Instant ahora = Instant.now();
		return Jwts.builder()
				.issuer(propiedades.getJwtEmisor())
				.subject(usuario.getId())
				.claim(CLAIM_TENANT, usuario.getTenant().getId())
				.claim(CLAIM_TIPO, TIPO_REFRESCO)
				.issuedAt(Date.from(ahora))
				.expiration(Date.from(ahora.plus(propiedades.getJwtDuracionRefrescoDias(), ChronoUnit.DAYS)))
				.signWith(clave)
				.compact();
	}

	public PrincipalNextDocs leerAcceso(String token) {
		Claims claims = leer(token);
		if (!TIPO_ACCESO.equals(claims.get(CLAIM_TIPO, String.class))) {
			throw new NoAutorizadoException("El token no es de acceso");
		}
		PrincipalNextDocs principal = new PrincipalNextDocs();
		principal.setIdActor(claims.getSubject());
		principal.setTipoActor(TipoActor.USUARIO);
		principal.setTenantId(claims.get(CLAIM_TENANT, String.class));
		principal.setCodigoTenant(claims.get(CLAIM_CODIGO_TENANT, String.class));
		principal.setPermisos(permisosDeClaims(claims));
		return principal;
	}

	public String leerSujetoRefresco(String token) {
		Claims claims = leer(token);
		if (!TIPO_REFRESCO.equals(claims.get(CLAIM_TIPO, String.class))) {
			throw new NoAutorizadoException("El token no es de refresco");
		}
		return claims.getSubject();
	}

	public String extraerDeCabecera(String cabecera) {
		if (cabecera == null || !cabecera.startsWith(PREFIJO_BEARER)) {
			return null;
		}
		return cabecera.substring(PREFIJO_BEARER.length()).trim();
	}

	private Claims leer(String token) {
		try {
			return Jwts.parser().verifyWith(clave).requireIssuer(propiedades.getJwtEmisor()).build()
					.parseSignedClaims(token).getPayload();
		} catch (JwtException | IllegalArgumentException e) {
			throw new NoAutorizadoException("Token invalido o expirado");
		}
	}

	private Set<String> permisosDe(Usuario usuario) {
		Set<String> permisos = new LinkedHashSet<>();
		for (Rol rol : usuario.getRoles()) {
			permisos.addAll(rol.getPermisos());
		}
		return permisos;
	}

	@SuppressWarnings("unchecked")
	private Set<String> permisosDeClaims(Claims claims) {
		Object valor = claims.get(CLAIM_PERMISOS);
		if (valor instanceof List<?> lista) {
			return new LinkedHashSet<>((List<String>) lista);
		}
		return new LinkedHashSet<>();
	}
}
