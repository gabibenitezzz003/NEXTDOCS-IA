package com.nextdocs.ai.enumeraciones;

public enum PlantillaWhatsapp {

	SOLICITUD_DOCUMENTACION,
	ACUSE_RECIBO,
	AVISO_MEDIA_RECHAZADA,
	AVISO_SIN_CORRELACION;

	public static PlantillaWhatsapp desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (PlantillaWhatsapp elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
