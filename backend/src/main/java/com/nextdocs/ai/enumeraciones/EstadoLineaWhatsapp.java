package com.nextdocs.ai.enumeraciones;

public enum EstadoLineaWhatsapp {

	ACTIVO,
	PAUSADO,
	ERROR;

	public static EstadoLineaWhatsapp desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstadoLineaWhatsapp elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
