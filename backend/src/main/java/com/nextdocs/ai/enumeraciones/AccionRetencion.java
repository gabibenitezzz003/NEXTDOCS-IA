package com.nextdocs.ai.enumeraciones;

public enum AccionRetencion {

	CONSERVAR,
	ANONIMIZAR,
	ELIMINAR;

	public static AccionRetencion desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (AccionRetencion elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
