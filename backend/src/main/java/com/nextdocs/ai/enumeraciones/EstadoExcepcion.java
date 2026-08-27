package com.nextdocs.ai.enumeraciones;

public enum EstadoExcepcion {

	ABIERTA,
	EN_CURSO,
	RESUELTA,
	DESCARTADA;

	public static EstadoExcepcion desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstadoExcepcion elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
