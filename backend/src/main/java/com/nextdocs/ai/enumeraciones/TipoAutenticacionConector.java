package com.nextdocs.ai.enumeraciones;

public enum TipoAutenticacionConector {

	NINGUNA,
	CLAVE_API,
	BEARER;

	public static TipoAutenticacionConector desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (TipoAutenticacionConector elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
