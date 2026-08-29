package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import lombok.Data;

@Data
public class MensajeWhatsappSalienteModel implements Serializable {

	private static final long serialVersionUID = 1147963230058714472L;

	private String id;

	private String lineaId;

	private String correlacionId;

	private String plantilla;

	private String numeroDestino;

	private String identificadorMensaje;

	private String estado;

	private boolean dentroDeVentana;

	private String nombrePlantillaMeta;

	private String cuerpo;

	private String detalleError;

	private Instant enviado;

	private Instant alta;
}
