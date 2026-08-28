package com.nextdocs.ai.enumeraciones;

public enum AccionPresupuestoCosto {

	ALERTA,
	BLOQUEAR_INGESTA;

	public static AccionPresupuestoCosto desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (AccionPresupuestoCosto elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
