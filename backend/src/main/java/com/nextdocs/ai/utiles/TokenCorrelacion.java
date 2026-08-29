package com.nextdocs.ai.utiles;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TokenCorrelacion {

	public static final String PREFIJO = "NDA-";

	private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

	private static final int LARGO = 16;

	private static final Pattern PATRON = Pattern.compile(PREFIJO + "[A-HJ-NP-Z2-9]{" + LARGO + "}");

	private static final SecureRandom AZAR = new SecureRandom();

	private TokenCorrelacion() {
	}

	public static String generar() {
		StringBuilder token = new StringBuilder(PREFIJO);
		for (int i = 0; i < LARGO; i++) {
			token.append(ALFABETO.charAt(AZAR.nextInt(ALFABETO.length())));
		}
		return token.toString();
	}

	public static String etiquetar(String token) {
		return "[" + token + "]";
	}

	public static Optional<String> detectar(String... textos) {
		for (String texto : textos) {
			if (texto == null || texto.isBlank()) {
				continue;
			}
			Matcher coincidencia = PATRON.matcher(texto.toUpperCase(java.util.Locale.ROOT));
			if (coincidencia.find()) {
				return Optional.of(coincidencia.group());
			}
		}
		return Optional.empty();
	}

	public static boolean tieneFormato(String token) {
		return token != null && PATRON.matcher(token.trim().toUpperCase(java.util.Locale.ROOT)).matches();
	}
}
