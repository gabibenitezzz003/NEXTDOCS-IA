package com.nextdocs.ai.enumeraciones;

public enum TipoReglaValidacion {

	OBLIGATORIO,
	FORMATO,
	RANGO,
	VIGENCIA,
	COMPARACION_CAMPOS,
	CATALOGO,
	EXPRESION,
	CONFIANZA_MINIMA;

	public static TipoReglaValidacion desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (TipoReglaValidacion elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
