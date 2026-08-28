package com.nextdocs.ai.enumeraciones;

public enum ResultadoEscaneo {

	LIMPIO,
	INFECTADO,
	NO_ANALIZADO,
	ERROR;

	public static ResultadoEscaneo desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (ResultadoEscaneo elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
