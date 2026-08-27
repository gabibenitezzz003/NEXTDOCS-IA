package com.nextdocs.ai.exceptions;

public class EntidadNoEncontradaException extends RuntimeException {

	private static final long serialVersionUID = 2295541124787332608L;

	public EntidadNoEncontradaException(String mensaje) {
		super(mensaje);
	}

	public static EntidadNoEncontradaException de(String entidad, String id) {
		return new EntidadNoEncontradaException("No se encontro " + entidad + " con id " + id);
	}
}
