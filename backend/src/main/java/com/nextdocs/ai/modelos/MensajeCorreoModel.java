package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class MensajeCorreoModel implements Serializable {

	private static final long serialVersionUID = 6698402172256099445L;

	private String id;

	private String buzonId;

	private String buzonDireccion;

	private String identificadorMensaje;

	private String remitente;

	private String destinatarios;

	private String asunto;

	private String tokenDetectado;

	private String correlacionId;

	private String resultado;

	private String motivo;

	private int adjuntos;

	private int ingestados;

	private int rechazados;

	private Instant enviadoEn;

	private Instant alta;

	private List<AdjuntoCorreoModel> detalleAdjuntos = new ArrayList<>();
}
