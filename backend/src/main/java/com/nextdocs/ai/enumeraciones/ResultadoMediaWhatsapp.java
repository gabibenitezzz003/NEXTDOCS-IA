package com.nextdocs.ai.enumeraciones;

public enum ResultadoMediaWhatsapp {

	INGESTADO,
	EN_CUARENTENA,
	RECHAZADO,
	ERROR;

	public static ResultadoMediaWhatsapp desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (ResultadoMediaWhatsapp elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
