package com.nextdocs.ai.enumeraciones;

public enum ResultadoMensajeCorreo {

	INGESTADO,
	PARCIAL,
	SIN_ADJUNTOS,
	REMITENTE_NO_AUTORIZADO,
	SIN_CORRELACION,
	RECHAZADO,
	ERROR;

	public static ResultadoMensajeCorreo desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (ResultadoMensajeCorreo elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
