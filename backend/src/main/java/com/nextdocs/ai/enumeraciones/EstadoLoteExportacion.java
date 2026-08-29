package com.nextdocs.ai.enumeraciones;

public enum EstadoLoteExportacion {

	SOLICITADO,
	GENERANDO,
	DISPONIBLE,
	VENCIDO,
	FALLIDO;

	public static EstadoLoteExportacion desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstadoLoteExportacion elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
