package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class CorreoCrudoModel implements Serializable {

	private static final long serialVersionUID = 3308911562074501229L;

	private String identificadorMensaje;

	private String remitente;

	private List<String> destinatarios = new ArrayList<>();

	private String asunto;

	private Instant enviadoEn;

	private List<AdjuntoCrudoModel> adjuntos = new ArrayList<>();

	@Data
	public static class AdjuntoCrudoModel implements Serializable {

		private static final long serialVersionUID = 6640827731174558020L;

		private String nombre;

		private String tipoMimeDeclarado;

		private byte[] contenido;
	}
}
