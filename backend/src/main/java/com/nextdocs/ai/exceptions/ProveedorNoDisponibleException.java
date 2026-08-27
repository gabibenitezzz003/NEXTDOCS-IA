package com.nextdocs.ai.exceptions;

public class ProveedorNoDisponibleException extends RuntimeException {

	private static final long serialVersionUID = 5583026791734502238L;

	private final boolean reintentable;

	public ProveedorNoDisponibleException(String mensaje, boolean reintentable) {
		super(mensaje);
		this.reintentable = reintentable;
	}

	public ProveedorNoDisponibleException(String mensaje, boolean reintentable, Throwable causa) {
		super(mensaje, causa);
		this.reintentable = reintentable;
	}

	public boolean esReintentable() {
		return reintentable;
	}
}
