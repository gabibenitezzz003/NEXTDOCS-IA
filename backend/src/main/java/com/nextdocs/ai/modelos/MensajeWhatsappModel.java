package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class MensajeWhatsappModel implements Serializable {

	private static final long serialVersionUID = 8850337299511044624L;

	private String id;

	private String lineaId;

	private String numeroLinea;

	private String identificadorMensaje;

	private String numeroOrigen;

	private String nombrePerfil;

	private String tipo;

	private String texto;

	private String tokenDetectado;

	private String correlacionId;

	private String resultado;

	private String motivo;

	private int media;

	private int ingestados;

	private int rechazados;

	private Instant recibidoEn;

	private Instant alta;

	private List<MediaWhatsappModel> detalleMedia = new ArrayList<>();
}
