package com.nextdocs.ai.utiles;

import java.util.regex.Pattern;

public final class NumeroTelefono {

	public static final String COMODIN = "*";

	private static final Pattern SOBRANTE = Pattern.compile("[\\s()\\-.]");

	private static final Pattern VALIDO = Pattern.compile("\\+[1-9][0-9]{7,17}");

	private static final Pattern PATRON_VALIDO = Pattern.compile("\\+[1-9][0-9]{0,17}\\*?");

	private NumeroTelefono() {
	}

	public static String normalizar(String numero) {
		if (numero == null || numero.isBlank()) {
			return null;
		}
		String limpio = SOBRANTE.matcher(numero.trim()).replaceAll("");
		if (limpio.startsWith("00")) {
			limpio = "+" + limpio.substring(2);
		}
		if (!limpio.startsWith("+")) {
			limpio = "+" + limpio;
		}
		return VALIDO.matcher(limpio).matches() ? limpio : null;
	}

	public static String normalizarDeclarado(String numero) {
		if (numero == null || numero.isBlank()) {
			return null;
		}
		String limpio = SOBRANTE.matcher(numero.trim()).replaceAll("");
		if (!limpio.startsWith("+") && !limpio.startsWith("00")) {
			return null;
		}
		return normalizar(limpio);
	}

	public static boolean esPatronValido(String patron) {
		if (patron == null || patron.isBlank()) {
			return false;
		}
		String limpio = SOBRANTE.matcher(patron.trim()).replaceAll("");
		return PATRON_VALIDO.matcher(limpio).matches();
	}

	public static String normalizarPatron(String patron) {
		if (!esPatronValido(patron)) {
			return null;
		}
		return SOBRANTE.matcher(patron.trim()).replaceAll("");
	}

	public static boolean coincide(String patron, String numero) {
		String patronLimpio = normalizarPatron(patron);
		String numeroLimpio = normalizar(numero);
		if (patronLimpio == null || numeroLimpio == null) {
			return false;
		}
		if (patronLimpio.endsWith(COMODIN)) {
			return numeroLimpio.startsWith(patronLimpio.substring(0, patronLimpio.length() - 1));
		}
		return patronLimpio.equals(numeroLimpio);
	}

	public static String enmascarar(String numero) {
		String limpio = normalizar(numero);
		if (limpio == null) {
			return "****";
		}
		int visibles = 4;
		return limpio.substring(0, limpio.length() - visibles).replaceAll("[0-9]", "*")
				+ limpio.substring(limpio.length() - visibles);
	}
}
