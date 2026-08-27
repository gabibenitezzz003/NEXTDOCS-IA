package com.nextdocs.ai.exceptions;

public class RegistroExistenteException extends RuntimeException {

	private static final long serialVersionUID = 1194886615285540023L;

	public RegistroExistenteException(String mensaje) {
		super(mensaje);
	}
}
