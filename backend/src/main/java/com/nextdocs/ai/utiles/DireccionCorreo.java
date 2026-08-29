package com.nextdocs.ai.utiles;

import java.util.Locale;

public final class DireccionCorreo {

	private DireccionCorreo() {
	}

	public static String normalizar(String direccion) {
		if (direccion == null) {
			return null;
		}
		String limpia = direccion.trim();
		int apertura = limpia.lastIndexOf('<');
		int cierre = limpia.lastIndexOf('>');
		if (apertura >= 0 && cierre > apertura) {
			limpia = limpia.substring(apertura + 1, cierre).trim();
		}
		return limpia.isEmpty() ? null : limpia.toLowerCase(Locale.ROOT);
	}

	public static String sinEtiqueta(String direccion) {
		String normalizada = normalizar(direccion);
		if (normalizada == null) {
			return null;
		}
		int arroba = normalizada.indexOf('@');
		int mas = normalizada.indexOf('+');
		if (mas < 0 || arroba < 0 || mas > arroba) {
			return normalizada;
		}
		return normalizada.substring(0, mas) + normalizada.substring(arroba);
	}

	public static String etiqueta(String direccion) {
		String normalizada = normalizar(direccion);
		if (normalizada == null) {
			return null;
		}
		int arroba = normalizada.indexOf('@');
		int mas = normalizada.indexOf('+');
		if (mas < 0 || arroba < 0 || mas > arroba) {
			return null;
		}
		return normalizada.substring(mas + 1, arroba);
	}

	public static String dominio(String direccion) {
		String normalizada = normalizar(direccion);
		int arroba = normalizada == null ? -1 : normalizada.indexOf('@');
		return arroba < 0 ? null : normalizada.substring(arroba);
	}

	public static boolean coincide(String patron, String direccion) {
		String normalizada = normalizar(direccion);
		if (patron == null || normalizada == null) {
			return false;
		}
		String limpio = patron.trim().toLowerCase(Locale.ROOT);
		if (limpio.startsWith("@")) {
			return limpio.equals(dominio(normalizada));
		}
		return limpio.equals(normalizada) || limpio.equals(sinEtiqueta(normalizada));
	}
}
