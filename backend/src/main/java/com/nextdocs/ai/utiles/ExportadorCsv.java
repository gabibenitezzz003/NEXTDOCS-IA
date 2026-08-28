package com.nextdocs.ai.utiles;

import java.util.List;

public final class ExportadorCsv {

	public static final String SEPARADOR = ";";

	public static final String SALTO = "\r\n";

	private static final String COMILLA = "\"";

	private static final String COMILLA_ESCAPADA = "\"\"";

	private static final String PREFIJOS_PELIGROSOS = "=+-@\t\r";

	private final StringBuilder contenido = new StringBuilder();

	public ExportadorCsv(List<String> encabezados) {
		fila(encabezados);
	}

	public void fila(List<?> valores) {
		for (int posicion = 0; posicion < valores.size(); posicion++) {
			if (posicion > 0) {
				contenido.append(SEPARADOR);
			}
			contenido.append(celda(valores.get(posicion)));
		}
		contenido.append(SALTO);
	}

	public String contenido() {
		return contenido.toString();
	}

	public static String celda(Object valor) {
		if (valor == null) {
			return "";
		}
		String texto = neutralizar(valor.toString());
		if (texto.contains(SEPARADOR) || texto.contains(COMILLA) || texto.contains("\n") || texto.contains("\r")) {
			return COMILLA + texto.replace(COMILLA, COMILLA_ESCAPADA) + COMILLA;
		}
		return texto;
	}

	private static String neutralizar(String texto) {
		if (!texto.isEmpty() && PREFIJOS_PELIGROSOS.indexOf(texto.charAt(0)) >= 0) {
			return "'" + texto;
		}
		return texto;
	}
}
