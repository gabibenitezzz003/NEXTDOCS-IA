package com.nextdocs.ai.enumeraciones;

public enum EstadoCircuito {

	CERRADO,
	ABIERTO,
	SEMIABIERTO;

	public static EstadoCircuito desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstadoCircuito elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
