package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;

import lombok.Data;

@Data
public class ResultadoExtraccionModel implements Serializable {

	private static final long serialVersionUID = 3390118824461502273L;

	private ProveedorDocumentalIa proveedor;

	private String modelo;

	private String versionPrompt;

	private String versionEsquema;

	private String tipoDetectado;

	private String subtipoDetectado;

	private BigDecimal confianzaClasificacion;

	private int paginasProcesadas;

	private long tokensEntrada;

	private long tokensSalida;

	private BigDecimal costo;

	private String monedaCosto;

	private long duracionMilisegundos;

	private List<ValorCanonicoModel> valores = new ArrayList<>();

	private List<String> advertencias = new ArrayList<>();
}
