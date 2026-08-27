package com.nextdocs.ai.filtros;

import java.io.IOException;

import com.nextdocs.ai.utiles.ContextoCorrelacion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(1)
public class FiltroCorrelacion extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		String correlacion = ContextoCorrelacion.establecer(peticion.getHeader(ContextoCorrelacion.CABECERA));
		respuesta.setHeader(ContextoCorrelacion.CABECERA, correlacion);
		try {
			cadena.doFilter(peticion, respuesta);
		} finally {
			ContextoCorrelacion.limpiar();
		}
	}
}
