package com.nextdocs.ai.enumeraciones;

public enum TipoExcepcion {

	TECNICA,
	CUOTA_PROVEEDOR,
	CALIDAD_LECTURA,
	VALIDACION,
	ASOCIACION,
	SEGURIDAD,
	ARCHIVO_RECHAZADO,
	CONECTOR;

	public static TipoExcepcion desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (TipoExcepcion elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
