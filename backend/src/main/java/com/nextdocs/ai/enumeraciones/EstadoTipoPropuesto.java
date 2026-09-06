package com.nextdocs.ai.enumeraciones;

public enum EstadoTipoPropuesto {

	PENDIENTE,
	APROBADO,
	DESCARTADO;

	public static EstadoTipoPropuesto desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstadoTipoPropuesto elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
