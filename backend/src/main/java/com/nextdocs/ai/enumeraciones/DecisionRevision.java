package com.nextdocs.ai.enumeraciones;

public enum DecisionRevision {

	APROBAR,
	RECHAZAR,
	OBSERVAR,
	CORREGIR,
	REPROCESAR;

	public static DecisionRevision desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (DecisionRevision elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
