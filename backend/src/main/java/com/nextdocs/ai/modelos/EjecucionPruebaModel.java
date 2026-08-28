package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.ResultadoQualityGate;

import lombok.Data;

@Data
public class EjecucionPruebaModel implements Serializable {

	private static final long serialVersionUID = 6619037742155083362L;

	private String id;

	private String versionId;

	private int numeroVersion;

	private String huellaConjunto;

	private BigDecimal exactitud;

	private BigDecimal exactitudReferencia;

	private BigDecimal factorCalibracion;

	private ResultadoQualityGate resultado;

	private String motivo;

	private Instant alta;

	private List<ResultadoCasoPruebaModel> casos = new ArrayList<>();
}
