package com.nextdocs.ai.filtros;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.nextdocs.ai.entidades.Rol;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.TipoActor;
import com.nextdocs.ai.exceptions.NoAutorizadoException;
import com.nextdocs.ai.modelos.PrincipalNextDocs;
import com.nextdocs.ai.repositorios.UsuarioRepository;
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

	private final UsuarioRepository usuarioRepository;

	public FiltroAutenticacion(TokenService tokenService, CuentaServicioService cuentaServicioService,
			UsuarioRepository usuarioRepository) {
		this.tokenService = tokenService;
		this.cuentaServicioService = cuentaServicioService;
		this.usuarioRepository = usuarioRepository;
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
			establecerContexto(revalidar(tokenService.leerAcceso(token)));
		}
	}

	private PrincipalNextDocs revalidar(PrincipalNextDocs principal) {
		if (principal.getTipoActor() != TipoActor.USUARIO) {
			return principal;
		}
		Usuario usuario = usuarioRepository.buscarHabilitadoConRoles(principal.getIdActor())
				.orElseThrow(() -> new NoAutorizadoException("El usuario ya no esta habilitado"));
		Set<String> permisos = new LinkedHashSet<>();
		for (Rol rol : usuario.getRoles()) {
			permisos.addAll(rol.getPermisos());
		}
		principal.setPermisos(permisos);
		return principal;
	}

	private void establecerContexto(PrincipalNextDocs principal) {
		List<SimpleGrantedAuthority> autoridades = principal.getPermisos().stream().map(SimpleGrantedAuthority::new)
				.toList();
		UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(principal, null,
				autoridades);
		SecurityContextHolder.getContext().setAuthentication(autenticacion);
	}
}
