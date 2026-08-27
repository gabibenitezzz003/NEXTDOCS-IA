package com.nextdocs.ai.enumeraciones;

public enum EstadoEventoSalida {

	PENDIENTE,
	ENVIADO,
	FALLIDO,
	DESCARTADO;

	public static EstadoEventoSalida desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstadoEventoSalida elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
