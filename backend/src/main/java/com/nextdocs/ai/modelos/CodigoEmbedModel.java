package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import lombok.Data;

@Data
public class CodigoEmbedModel implements Serializable {

	private static final long serialVersionUID = 7761204458830219644L;

	private String codigo;

	private Instant venceEn;

	private int segundosVigencia;

	private String usuarioId;

	private String email;

	private String codigoTenant;

	private boolean aprovisionado;

	private String aplicacionOrigen;

	private String tipoObjeto;

	private String idObjeto;

	private String urlRetorno;
}
