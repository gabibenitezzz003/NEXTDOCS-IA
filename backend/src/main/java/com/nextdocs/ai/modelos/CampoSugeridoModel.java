package com.nextdocs.ai.modelos;

import java.io.Serializable;

import com.nextdocs.ai.enumeraciones.TipoDatoCampo;

import lombok.Data;

@Data
public class CampoSugeridoModel implements Serializable {

	private static final long serialVersionUID = 8837490211564430097L;

	private String clave;

	private String etiqueta;

	private TipoDatoCampo tipoDato;

	private boolean requerido;

	private String ejemplo;
}
