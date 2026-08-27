package com.nextdocs.ai.filtros;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.exceptions.NoAutorizadoException;
import com.nextdocs.ai.modelos.PrincipalNextDocs;
import com.nextdocs.ai.servicios.CuentaServicioService;
import com.nextdocs.ai.servicios.jwt.TokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FiltroAutenticacion extends OncePerRequestFilter {

	private final TokenService tokenService;

	private final CuentaServicioService cuentaServicioService;

	public FiltroAutenticacion(TokenService tokenService, CuentaServicioService cuentaServicioService) {
		this.tokenService = tokenService;
		this.cuentaServicioService = cuentaServicioService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				autenticar(peticion);
			} catch (NoAutorizadoException e) {
				SecurityContextHolder.clearContext();
			}
		}
		cadena.doFilter(peticion, respuesta);
	}

	private void autenticar(HttpServletRequest peticion) {
		String claveServicio = peticion.getHeader(CuentaServicioService.CABECERA_CLAVE);
		if (claveServicio != null && !claveServicio.isBlank()) {
			Optional<PrincipalNextDocs> principal = cuentaServicioService.autenticar(claveServicio);
			principal.ifPresent(this::establecerContexto);
			principal.ifPresent(valor -> cuentaServicioService.registrarUso(valor.getIdActor()));
			return;
		}
		String token = tokenService.extraerDeCabecera(peticion.getHeader(HttpHeaders.AUTHORIZATION));
		if (token != null) {
			establecerContexto(tokenService.leerAcceso(token));
		}
	}

	private void establecerContexto(PrincipalNextDocs principal) {
		List<SimpleGrantedAuthority> autoridades = principal.getPermisos().stream().map(SimpleGrantedAuthority::new)
				.toList();
		UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(principal, null,
				autoridades);
		SecurityContextHolder.getContext().setAuthentication(autenticacion);
	}
}
