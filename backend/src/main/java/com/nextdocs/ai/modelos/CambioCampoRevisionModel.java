package com.nextdocs.ai.modelos;

import java.io.Serializable;

import lombok.Data;

@Data
public class CambioCampoRevisionModel implements Serializable {

	private static final long serialVersionUID = 8850233691047260125L;

	private String claveCampo;

	private String valorAnterior;

	private String valorNuevo;

	private String motivo;
}
