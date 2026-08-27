package com.nextdocs.ai.enumeraciones;

public enum SensibilidadCampo {

	PUBLICA,
	INTERNA,
	CONFIDENCIAL,
	PERSONAL;

	public static SensibilidadCampo desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (SensibilidadCampo elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
