package com.nextdocs.ai.enumeraciones;

public enum MotorAntivirus {

	PERMISIVO,
	CLAMAV;

	public static MotorAntivirus desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (MotorAntivirus elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
