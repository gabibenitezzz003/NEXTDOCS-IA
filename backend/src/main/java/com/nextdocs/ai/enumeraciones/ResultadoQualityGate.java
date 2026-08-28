package com.nextdocs.ai.enumeraciones;

public enum ResultadoQualityGate {

	APROBADO,
	RECHAZADO;

	public static ResultadoQualityGate desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (ResultadoQualityGate elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
