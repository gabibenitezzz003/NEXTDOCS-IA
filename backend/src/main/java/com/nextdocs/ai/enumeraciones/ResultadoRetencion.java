package com.nextdocs.ai.enumeraciones;

public enum ResultadoRetencion {

	APLICADA,
	OMITIDA_POR_RETENCION_LEGAL,
	OMITIDA_SIN_POLITICA,
	OMITIDA_NO_VENCIDA,
	OMITIDA_YA_APLICADA;

	public static ResultadoRetencion desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (ResultadoRetencion elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
