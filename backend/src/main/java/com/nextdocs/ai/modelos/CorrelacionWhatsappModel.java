package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import lombok.Data;

@Data
public class CorrelacionWhatsappModel implements Serializable {

	private static final long serialVersionUID = 6098320174455130026L;

	private String id;

	private String token;

	private String textoParaEnviar;

	private String lineaId;

	private String numeroLinea;

	private String sujetoOrigen;

	private String sujetoTipoObjeto;

	private String sujetoIdObjeto;

	private String codigoPlantilla;

	private String numeroDestino;

	private String numeroVinculado;

	private Instant vinculadoEn;

	private String descripcion;

	private Instant venceEn;

	private boolean vigente;

	private int documentosRecibidos;

	private Instant ultimoUso;

	private Instant alta;

	private String mensajeSalienteId;
}
