package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import lombok.Data;

@Data
public class MensajeSalienteModel implements Serializable {

	private static final long serialVersionUID = 8846521907711045300L;

	private String id;

	private String buzonId;

	private String correlacionId;

	private String plantilla;

	private String destinatarios;

	private String asunto;

	private String identificadorMensaje;

	private String estado;

	private String detalleError;

	private Instant enviado;

	private Instant alta;
}
