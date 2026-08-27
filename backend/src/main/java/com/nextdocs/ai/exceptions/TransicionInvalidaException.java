package com.nextdocs.ai.exceptions;

import com.nextdocs.ai.enumeraciones.EstadoDocumento;

public class TransicionInvalidaException extends RuntimeException {

	private static final long serialVersionUID = 7620345891122764430L;

	public TransicionInvalidaException(EstadoDocumento origen, EstadoDocumento destino) {
		super("Transicion no permitida de " + origen + " a " + destino);
	}
}
