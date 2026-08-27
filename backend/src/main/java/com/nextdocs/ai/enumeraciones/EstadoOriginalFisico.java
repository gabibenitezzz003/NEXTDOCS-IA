package com.nextdocs.ai.enumeraciones;

public enum EstadoOriginalFisico {

	NO_REQUERIDO,
	PENDIENTE,
	RECIBIDO,
	ARCHIVADO,
	EXTRAVIADO;

	public static EstadoOriginalFisico desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstadoOriginalFisico elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
