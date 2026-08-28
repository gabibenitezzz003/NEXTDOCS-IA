package com.nextdocs.ai.utiles;

import java.util.Optional;

public final class ResolvedorSecreto {

	public static final String PREFIJO_ENTORNO = "env:";

	public static final String PREFIJO_LITERAL = "literal:";

	private ResolvedorSecreto() {
	}

	public static Optional<String> resolver(String referencia) {
		if (referencia == null || referencia.isBlank()) {
			return Optional.empty();
		}
		String limpia = referencia.trim();
		if (limpia.startsWith(PREFIJO_ENTORNO)) {
			return desdeEntorno(limpia.substring(PREFIJO_ENTORNO.length()));
		}
		if (limpia.startsWith(PREFIJO_LITERAL)) {
			return valorNoVacio(limpia.substring(PREFIJO_LITERAL.length()));
		}
		return desdeEntorno(limpia);
	}

	public static String enmascarar(String secreto) {
		if (secreto == null || secreto.length() < 8) {
			return "****";
		}
		return secreto.substring(0, 4) + "****" + secreto.substring(secreto.length() - 4);
	}

	private static Optional<String> desdeEntorno(String nombreVariable) {
		String valor = System.getenv(nombreVariable);
		if (valor == null || valor.isBlank()) {
			valor = System.getProperty(nombreVariable);
		}
		return valorNoVacio(valor);
	}

	private static Optional<String> valorNoVacio(String valor) {
		return valor == null || valor.isBlank() ? Optional.empty() : Optional.of(valor.trim());
	}
}
