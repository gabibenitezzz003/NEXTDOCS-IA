package com.nextdocs.ai.enumeraciones;

public enum ResultadoAsociacion {

	NO_APLICA,
	SIN_CANDIDATOS,
	RESUELTA,
	AMBIGUA,
	CONECTOR_FALLIDO;

	public static ResultadoAsociacion desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (ResultadoAsociacion elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
