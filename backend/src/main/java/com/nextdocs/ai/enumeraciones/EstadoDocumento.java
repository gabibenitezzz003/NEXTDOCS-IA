package com.nextdocs.ai.enumeraciones;

public enum EstadoDocumento {

	RECIBIDO,
	PROCESANDO,
	EXTRAIDO,
	VALIDADO,
	OBSERVADO,
	APROBADO,
	RECHAZADO,
	CERRADO,
	DIVIDIDO;

	public static EstadoDocumento desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstadoDocumento elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
