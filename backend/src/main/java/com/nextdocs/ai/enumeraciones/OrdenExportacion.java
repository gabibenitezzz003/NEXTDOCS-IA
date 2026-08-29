package com.nextdocs.ai.enumeraciones;

public enum OrdenExportacion {

	FECHA_DESC("recibido", false),
	FECHA_ASC("recibido", true),
	TAMANO_DESC("tamano", false),
	ESTADO_ASC("estado", true);

	private final String campo;

	private final boolean ascendente;

	OrdenExportacion(String campo, boolean ascendente) {
		this.campo = campo;
		this.ascendente = ascendente;
	}

	public String getCampo() {
		return campo;
	}

	public boolean isAscendente() {
		return ascendente;
	}

	public static OrdenExportacion desde(String valor) {
		if (valor == null) {
			return FECHA_DESC;
		}
		for (OrdenExportacion elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return FECHA_DESC;
	}
}
