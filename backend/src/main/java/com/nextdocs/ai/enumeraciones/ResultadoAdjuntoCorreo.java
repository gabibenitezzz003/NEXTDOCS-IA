package com.nextdocs.ai.enumeraciones;

public enum ResultadoAdjuntoCorreo {

	INGESTADO,
	EN_CUARENTENA,
	RECHAZADO,
	ERROR;

	public static ResultadoAdjuntoCorreo desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (ResultadoAdjuntoCorreo elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
