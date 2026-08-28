package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ResumenCostoModel implements Serializable {

	private static final long serialVersionUID = 7712066429318504286L;

	private Instant desde;

	private Instant hasta;

	private String moneda;

	private long documentosRecibidos;

	private long documentosCorrectos;

	private long extracciones;

	private long extraccionesCompletadas;

	private long extraccionesFallidas;

	private long tokensEntrada;

	private long tokensSalida;

	private BigDecimal costoInferencia;

	private BigDecimal costoEfectivoPorDocumentoCorrecto;

	private long duracionMediaMilisegundos;

	private PoliticaCostoModel politica;

	private BigDecimal gastadoEnPeriodoPresupuesto;

	private BigDecimal porcentajePresupuesto;

	private boolean alerta;

	private boolean bloqueaIngesta;

	private List<CostoAgrupadoModel> porProveedor = new ArrayList<>();

	private List<CostoAgrupadoModel> porPlantilla = new ArrayList<>();
}
