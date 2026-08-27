package com.nextdocs.ai.enumeraciones;

public enum EstadoTenant {

	ACTIVO,
	SUSPENDIDO,
	BAJA;

	public static EstadoTenant desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstadoTenant elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
