package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ConjuntoPruebaModel implements Serializable {

	private static final long serialVersionUID = 9024417736512083365L;

	private String id;

	private String plantillaId;

	private String huella;

	private int cantidadCasos;

	private boolean exigirQualityGate;

	private BigDecimal umbralMinimo;

	private Instant alta;

	private List<CasoPruebaModel> casos = new ArrayList<>();
}
