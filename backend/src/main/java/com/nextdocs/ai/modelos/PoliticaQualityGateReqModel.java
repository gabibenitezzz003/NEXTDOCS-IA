package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class PoliticaQualityGateReqModel implements Serializable {

	private static final long serialVersionUID = 4471028836519207742L;

	private boolean exigirQualityGate;

	private BigDecimal umbralMinimo;
}
