package com.nextdocs.ai.exceptions;

public class ArchivoRechazadoException extends RuntimeException {

	private static final long serialVersionUID = 3300612245119870934L;

	private final String codigo;

	public ArchivoRechazadoException(String codigo, String mensaje) {
		super(mensaje);
		this.codigo = codigo;
	}

	public String getCodigo() {
		return codigo;
	}
}
