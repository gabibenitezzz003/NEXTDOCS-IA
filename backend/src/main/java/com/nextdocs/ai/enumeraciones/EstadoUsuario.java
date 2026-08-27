package com.nextdocs.ai.enumeraciones;

public enum EstadoUsuario {

	ACTIVO,
	INVITADO,
	BLOQUEADO,
	BAJA;

	public static EstadoUsuario desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstadoUsuario elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
