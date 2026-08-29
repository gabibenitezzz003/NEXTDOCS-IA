package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import lombok.Data;

@Data
public class CorrelacionCorreoModel implements Serializable {

	private static final long serialVersionUID = 9188724566037045512L;

	private String id;

	private String token;

	private String etiquetaAsunto;

	private String direccionConEtiqueta;

	private String sujetoOrigen;

	private String sujetoTipoObjeto;

	private String sujetoIdObjeto;

	private String codigoPlantilla;

	private String destinatario;

	private String descripcion;

	private Instant venceEn;

	private boolean vigente;

	private int documentosRecibidos;

	private Instant ultimoUso;

	private Instant alta;

	private String mensajeSalienteId;
}
