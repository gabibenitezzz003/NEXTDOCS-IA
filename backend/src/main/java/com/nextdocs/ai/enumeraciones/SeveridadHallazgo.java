package com.nextdocs.ai.enumeraciones;

public enum SeveridadHallazgo {

	INFORMATIVO,
	ADVERTENCIA,
	REQUIERE_REVISION,
	BLOQUEANTE;

	public static SeveridadHallazgo desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (SeveridadHallazgo elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
