package com.nextdocs.ai.enumeraciones;

public enum ProveedorDocumentalIa {

	GEMINI,
	DEEPSEEK,
	ABBYY,
	SIMULADO;

	public static ProveedorDocumentalIa desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (ProveedorDocumentalIa elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
