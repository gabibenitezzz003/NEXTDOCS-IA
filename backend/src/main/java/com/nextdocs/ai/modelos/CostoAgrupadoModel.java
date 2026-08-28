package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class CostoAgrupadoModel implements Serializable {

	private static final long serialVersionUID = 2294417736512083372L;

	private String clave;

	private BigDecimal costo;

	private long extracciones;

	private long documentosCorrectos;
}
