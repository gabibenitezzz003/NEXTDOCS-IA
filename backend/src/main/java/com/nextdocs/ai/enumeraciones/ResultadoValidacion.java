package com.nextdocs.ai.enumeraciones;

public enum ResultadoValidacion {

	APROBADO,
	OBSERVADO,
	RECHAZADO;

	public static ResultadoValidacion desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (ResultadoValidacion elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
