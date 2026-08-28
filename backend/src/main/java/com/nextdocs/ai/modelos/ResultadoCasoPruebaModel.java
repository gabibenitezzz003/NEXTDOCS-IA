package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class ResultadoCasoPruebaModel implements Serializable {

	private static final long serialVersionUID = 2295541124787332609L;

	private String casoId;

	private String nombre;

	private int aciertos;

	private int total;

	private BigDecimal exactitud;

	private String extraido;
}
