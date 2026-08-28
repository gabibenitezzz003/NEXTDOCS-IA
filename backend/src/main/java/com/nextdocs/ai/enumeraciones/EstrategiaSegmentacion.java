package com.nextdocs.ai.enumeraciones;

public enum EstrategiaSegmentacion {

	NINGUNA,
	PAGINAS_FIJAS,
	PATRON_TEXTO;

	public static EstrategiaSegmentacion desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstrategiaSegmentacion elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
