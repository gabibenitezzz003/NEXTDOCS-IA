package com.nextdocs.ai.filtros;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.errores.WebErrorModel;
import com.nextdocs.ai.modelos.PrincipalNextDocs;
import com.nextdocs.ai.modelos.ResultadoLimiteModel;
import com.nextdocs.ai.servicios.LimiteUsoService;
import com.nextdocs.ai.utiles.ContextoCorrelacion;
import com.nextdocs.ai.utiles.ContextoSeguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(3)
public class FiltroLimiteUso extends OncePerRequestFilter {

	public static final String CABECERA_LIMITE = "X-Limite-Uso";

	public static final String CABECERA_RESTANTES = "X-Limite-Restantes";

	public static final String CABECERA_REINTENTAR = "Retry-After";

	private static final Logger log = LoggerFactory.getLogger(FiltroLimiteUso.class);

	private final LimiteUsoService limiteUsoService;

	private final ObjectMapper objectMapper;

	public FiltroLimiteUso(LimiteUsoService limiteUsoService, ObjectMapper objectMapper) {
		this.limiteUsoService = limiteUsoService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		if (!limiteUsoService.estaActivo() || limiteUsoService.esRutaExenta(peticion.getRequestURI())) {
			cadena.doFilter(peticion, respuesta);
			return;
		}
		PrincipalNextDocs principal = ContextoSeguridad.principalONulo();
		if (principal == null) {
			cadena.doFilter(peticion, respuesta);
			return;
		}

		ResultadoLimiteModel resultado = evaluar(peticion, principal);
		respuesta.setHeader(CABECERA_LIMITE, String.valueOf(resultado.getLimite()));
		respuesta.setHeader(CABECERA_RESTANTES, String.valueOf(resultado.getRestantes()));
		if (resultado.isPermitido()) {
			cadena.doFilter(peticion, respuesta);
			return;
		}
		rechazar(peticion, respuesta, resultado, principal);
	}

	private ResultadoLimiteModel evaluar(HttpServletRequest peticion, PrincipalNextDocs principal) {
		ResultadoLimiteModel porPrincipal = limiteUsoService.consumir(LimiteUsoService.ALCANCE_PRINCIPAL,
				principal.getIdActor(), limiteUsoService.limitePorPrincipal());
		if (!porPrincipal.isPermitido()) {
			return porPrincipal;
		}
		ResultadoLimiteModel porTenant = limiteUsoService.consumir(LimiteUsoService.ALCANCE_TENANT,
				principal.getTenantId(), limiteUsoService.limitePorTenant());
		if (!porTenant.isPermitido()) {
			return porTenant;
		}
		if (!limiteUsoService.esRutaDeIngesta(peticion.getRequestURI())) {
			return porPrincipal;
		}
		ResultadoLimiteModel porIngesta = limiteUsoService.consumir(LimiteUsoService.ALCANCE_INGESTA,
				principal.getTenantId(), limiteUsoService.limiteDeIngesta());
		return porIngesta.isPermitido() ? porPrincipal : porIngesta;
	}

	private void rechazar(HttpServletRequest peticion, HttpServletResponse respuesta,
			ResultadoLimiteModel resultado, PrincipalNextDocs principal) throws IOException {
		log.warn("Limite de uso excedido por {} del tenant {} en {} (alcance {})", principal.getIdActor(),
				principal.getTenantId(), peticion.getRequestURI(), resultado.getAlcanceExcedido());
		respuesta.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		respuesta.setHeader(CABECERA_REINTENTAR, String.valueOf(resultado.getEsperaSegundos()));
		respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
		WebErrorModel error = new WebErrorModel(
				"Se supero el limite de " + resultado.getLimite() + " peticiones por minuto por "
						+ resultado.getAlcanceExcedido() + ". Reintenta en " + resultado.getEsperaSegundos()
						+ " segundos",
				peticion.getRequestURI(), HttpStatus.TOO_MANY_REQUESTS, ContextoCorrelacion.obtener());
		respuesta.getWriter().write(objectMapper.writeValueAsString(error));
	}
}
