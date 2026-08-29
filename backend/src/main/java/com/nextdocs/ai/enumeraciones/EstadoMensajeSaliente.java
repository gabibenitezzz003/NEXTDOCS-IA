package com.nextdocs.ai.enumeraciones;

public enum EstadoMensajeSaliente {

	PENDIENTE,
	ENVIADO,
	FALLIDO;

	public static EstadoMensajeSaliente desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstadoMensajeSaliente elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
