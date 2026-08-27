package com.nextdocs.ai.enumeraciones;

public enum EstadoEjecucion {

	PENDIENTE,
	EJECUTANDO,
	COMPLETADA,
	FALLIDA,
	CANCELADA;

	public static EstadoEjecucion desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstadoEjecucion elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
