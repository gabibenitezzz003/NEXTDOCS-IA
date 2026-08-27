package com.nextdocs.ai.utiles;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

public final class ClaveObjeto {

	private static final int LONGITUD_MAXIMA_NOMBRE = 80;

	private ClaveObjeto() {
	}

	public static String paraDocumento(String tenantId, String documentoId, String nombreArchivo) {
		LocalDate fecha = LocalDate.now(ZoneOffset.UTC);
		return String.join("/", "tenant=" + tenantId, String.valueOf(fecha.getYear()),
				String.format("%02d", fecha.getMonthValue()), String.format("%02d", fecha.getDayOfMonth()), documentoId,
				sanear(nombreArchivo));
	}

	public static String paraExportacion(String tenantId, String exportacionId, String nombreArchivo) {
		return String.join("/", "tenant=" + tenantId, "exportaciones", exportacionId, sanear(nombreArchivo));
	}

	public static String sanear(String nombreArchivo) {
		if (nombreArchivo == null || nombreArchivo.isBlank()) {
			return UUID.randomUUID().toString();
		}
		String base = nombreArchivo.trim().replace("\\", "/");
		int separador = base.lastIndexOf('/');
		if (separador >= 0) {
			base = base.substring(separador + 1);
		}
		base = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "-").replaceAll("-{2,}", "-");
		if (base.length() > LONGITUD_MAXIMA_NOMBRE) {
			base = base.substring(base.length() - LONGITUD_MAXIMA_NOMBRE);
		}
		return base.isBlank() ? UUID.randomUUID().toString() : base;
	}

	public static String extension(String nombreArchivo) {
		if (nombreArchivo == null) {
			return null;
		}
		int punto = nombreArchivo.lastIndexOf('.');
		if (punto < 0 || punto == nombreArchivo.length() - 1) {
			return null;
		}
		return nombreArchivo.substring(punto + 1).toLowerCase(Locale.ROOT);
	}
}
