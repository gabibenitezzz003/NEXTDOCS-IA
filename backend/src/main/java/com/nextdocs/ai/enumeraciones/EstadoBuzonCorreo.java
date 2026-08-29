package com.nextdocs.ai.enumeraciones;

public enum EstadoBuzonCorreo {

	ACTIVO,
	PAUSADO,
	ERROR;

	public static EstadoBuzonCorreo desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstadoBuzonCorreo elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
