package com.nextdocs.ai.enumeraciones;

public enum ResultadoMensajeWhatsapp {

	INGESTADO,
	PARCIAL,
	SIN_MEDIA,
	CONTACTO_NO_AUTORIZADO,
	SIN_CORRELACION,
	CORRELACION_AMBIGUA,
	RECHAZADO,
	ERROR;

	public static ResultadoMensajeWhatsapp desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (ResultadoMensajeWhatsapp elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
