package com.nextdocs.ai.enumeraciones;

public enum EstadoPlantilla {

	BORRADOR,
	EN_PRUEBA,
	PUBLICADA,
	DEPRECADA;

	public static EstadoPlantilla desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstadoPlantilla elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
