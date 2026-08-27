package com.nextdocs.ai.enumeraciones;

public enum TipoActor {

	USUARIO,
	CUENTA_SERVICIO,
	SISTEMA,
	PROVEEDOR,
	EXTERNO;

	public static TipoActor desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (TipoActor elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
