package com.nextdocs.ai.utiles;

import java.util.UUID;

import org.slf4j.MDC;

public final class ContextoCorrelacion {

	public static final String CABECERA = "X-Correlacion-Id";

	public static final String CLAVE_MDC = "correlacionId";

	private ContextoCorrelacion() {
	}

	public static String establecer(String valor) {
		String correlacion = valor == null || valor.isBlank() ? generar() : valor.trim();
		MDC.put(CLAVE_MDC, correlacion);
		return correlacion;
	}

	public static String obtener() {
		return MDC.get(CLAVE_MDC);
	}

	public static String obtenerOGenerar() {
		String correlacion = obtener();
		return correlacion == null ? establecer(null) : correlacion;
	}

	public static void limpiar() {
		MDC.remove(CLAVE_MDC);
	}

	public static String generar() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
