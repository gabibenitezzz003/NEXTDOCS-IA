package com.nextdocs.ai.enumeraciones;

public enum PlantillaCorreo {

	SOLICITUD_DOCUMENTACION,
	ACUSE_RECIBO,
	AVISO_ADJUNTO_RECHAZADO,
	AVISO_REMITENTE_NO_AUTORIZADO,
	AVISO_SIN_CORRELACION;

	public static PlantillaCorreo desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (PlantillaCorreo elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
