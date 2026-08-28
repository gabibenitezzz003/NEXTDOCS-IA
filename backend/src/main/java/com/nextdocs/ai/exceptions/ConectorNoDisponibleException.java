package com.nextdocs.ai.exceptions;

public class ConectorNoDisponibleException extends RuntimeException {

	private static final long serialVersionUID = 4461208856690137725L;

	private final String codigoConector;

	private final boolean circuitoAbierto;

	public ConectorNoDisponibleException(String codigoConector, String mensaje, boolean circuitoAbierto) {
		super(mensaje);
		this.codigoConector = codigoConector;
		this.circuitoAbierto = circuitoAbierto;
	}

	public ConectorNoDisponibleException(String codigoConector, String mensaje, Throwable causa) {
		super(mensaje, causa);
		this.codigoConector = codigoConector;
		this.circuitoAbierto = false;
	}

	public String getCodigoConector() {
		return codigoConector;
	}

	public boolean estaCircuitoAbierto() {
		return circuitoAbierto;
	}
}
