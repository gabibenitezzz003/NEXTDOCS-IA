package com.nextdocs.ai.exceptions;

public class NoAutorizadoException extends RuntimeException {

	private static final long serialVersionUID = 4429097833155278294L;

	public NoAutorizadoException(String mensaje) {
		super(mensaje);
	}
}
