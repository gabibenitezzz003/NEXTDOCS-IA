package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.EstadoEjecucion;
import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;

import lombok.Data;

@Data
public class EjecucionExtraccionModel implements Serializable {

	private static final long serialVersionUID = 6613374429810570462L;

	private String id;

	private ProveedorDocumentalIa proveedor;

	private String modelo;

	private String versionPrompt;

	private String versionEsquema;

	private EstadoEjecucion estado;

	private int intento;

	private long tokensEntrada;

	private long tokensSalida;

	private int paginasProcesadas;

	private BigDecimal costo;

	private long duracionMilisegundos;

	private String codigoError;

	private String mensajeError;

	private Instant inicio;

	private Instant fin;

	private List<ValorExtraidoModel> valores = new ArrayList<>();
}
