package com.nextdocs.ai.enumeraciones;

public enum OrigenTipoDocumento {

	DECLARADO,
	DETECTADO,
	REVISION;

	public static OrigenTipoDocumento desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (OrigenTipoDocumento elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
