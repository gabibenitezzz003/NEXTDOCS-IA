package com.nextdocs.ai.enumeraciones;

public enum TipoEventoCanonico {

	DOCUMENTO_RECIBIDO("document.received"),
	DOCUMENTO_SEGMENTADO("document.segmented"),
	DOCUMENTO_EXTRAIDO("document.extracted"),
	DOCUMENTO_VALIDADO("document.validated"),
	DOCUMENTO_OBSERVADO("document.observed"),
	DOCUMENTO_APROBADO("document.approved"),
	DOCUMENTO_RECHAZADO("document.rejected"),
	DOCUMENTO_CERRADO("document.closed"),
	DOCUMENTO_VERSION_PUBLICADA("document.version_published"),
	EXTRACCION_FALLIDA("extraction.failed"),
	EXCEPCION_CREADA("exception.created"),
	EXCEPCION_RESUELTA("exception.resolved"),
	PLANTILLA_PUBLICADA("template.published"),
	PLANTILLA_DEPRECADA("template.deprecated"),
	ACCION_CONECTOR_FALLIDA("connector.action.failed"),
	EXPORTACION_LISTA("export.ready"),
	EXPORTACION_POR_VENCER("export.expiring"),
	EXPORTACION_VENCIDA("export.expired"),
	ALMACENAMIENTO_EN_UMBRAL("storage.threshold"),
	CORREO_RECIBIDO("mail.received"),
	CORREO_RECHAZADO("mail.rejected"),
	WHATSAPP_RECIBIDO("whatsapp.received"),
	WHATSAPP_RECHAZADO("whatsapp.rejected"),
	WEBHOOK_PRUEBA("webhook.test");

	private final String clave;

	TipoEventoCanonico(String clave) {
		this.clave = clave;
	}

	public String getClave() {
		return clave;
	}

	public static TipoEventoCanonico desde(String valor) {
		if (valor == null) {
			return null;
		}
		String normalizado = valor.trim();
		for (TipoEventoCanonico elemento : values()) {
			if (elemento.name().equalsIgnoreCase(normalizado) || elemento.clave.equalsIgnoreCase(normalizado)) {
				return elemento;
			}
		}
		return null;
	}
}
