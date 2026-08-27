package com.nextdocs.ai.enumeraciones;

public enum OrigenIdentidad {

	LOCAL,
	FOLLOW,
	CIMA,
	VALID360,
	OIDC;

	public static OrigenIdentidad desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (OrigenIdentidad elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
