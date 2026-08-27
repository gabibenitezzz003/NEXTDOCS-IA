package com.nextdocs.ai.enumeraciones;

public enum EstadoEntregaWebhook {

	PENDIENTE,
	ENTREGADO,
	FALLIDO,
	AGOTADO;

	public static EstadoEntregaWebhook desde(String valor) {
		if (valor == null) {
			return null;
		}
		for (EstadoEntregaWebhook elemento : values()) {
			if (elemento.name().equalsIgnoreCase(valor.trim())) {
				return elemento;
			}
		}
		return null;
	}
}
