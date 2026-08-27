package com.nextdocs.ai.enumeraciones;

public enum TipoDatoCampo {

	TEXTO,
	NUMERO,
	DECIMAL,
	FECHA,
	FECHA_HORA,
	BOOLEANO,
	MONEDA,
	LISTA,
	TABLA,
	CUIT,
	EMAIL,
	TELEFONO;

	public static TipoDatoCampo desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (TipoDatoCampo elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
