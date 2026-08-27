package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class CandidatoAsociacionModel implements Serializable {

	private static final long serialVersionUID = 2287049916630874005L;

	private String id;

	private String conector;

	private String origen;

	private String tipoObjeto;

	private String idObjeto;

	private String descripcion;

	private BigDecimal puntaje;

	private String razones;

	private boolean seleccionado;
}
