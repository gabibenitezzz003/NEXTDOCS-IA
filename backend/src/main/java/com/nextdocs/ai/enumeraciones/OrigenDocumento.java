package com.nextdocs.ai.enumeraciones;

public enum OrigenDocumento {

	WEB,
	API,
	EMAIL,
	WHATSAPP,
	SFTP,
	CONECTOR,
	DEMOSTRACION;

	public static OrigenDocumento desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (OrigenDocumento elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
