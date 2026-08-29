package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class MensajeWhatsappCrudoModel implements Serializable {

	private static final long serialVersionUID = 4483056970113672248L;

	private String identificadorMensaje;

	private String identificadorNumero;

	private String numeroOrigen;

	private String nombrePerfil;

	private String tipo;

	private String texto;

	private Instant recibidoEn;

	private List<MediaCrudaModel> media = new ArrayList<>();

	@Data
	public static class MediaCrudaModel implements Serializable {

		private static final long serialVersionUID = 6172440855109833024L;

		private String identificadorMedia;

		private String nombreDeclarado;

		private String tipoMimeDeclarado;

		private String descripcion;
	}
}
