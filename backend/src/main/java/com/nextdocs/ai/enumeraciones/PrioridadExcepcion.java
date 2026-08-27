package com.nextdocs.ai.enumeraciones;

public enum PrioridadExcepcion {

	BAJA,
	MEDIA,
	ALTA,
	CRITICA;

	public static PrioridadExcepcion desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (PrioridadExcepcion elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
