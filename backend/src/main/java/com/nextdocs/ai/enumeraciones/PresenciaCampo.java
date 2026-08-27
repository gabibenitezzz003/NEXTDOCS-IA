package com.nextdocs.ai.enumeraciones;

public enum PresenciaCampo {

	PRESENTE,
	NO_FIGURA,
	ILEGIBLE;

	public static PresenciaCampo desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (PresenciaCampo elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
