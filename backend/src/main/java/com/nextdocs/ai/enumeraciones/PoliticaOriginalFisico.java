package com.nextdocs.ai.enumeraciones;

public enum PoliticaOriginalFisico {

	NO_REQUIERE,
	REQUIERE_PARA_CIERRE,
	REQUIERE_SEGUIMIENTO;

	public static PoliticaOriginalFisico desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (PoliticaOriginalFisico elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
