package com.nextdocs.ai.exceptions;

public class ProhibidoException extends RuntimeException {

	private static final long serialVersionUID = 8871240955513371183L;

	public ProhibidoException(String mensaje) {
		super(mensaje);
	}
}
