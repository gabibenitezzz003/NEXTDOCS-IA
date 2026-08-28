package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;

import com.nextdocs.ai.enumeraciones.PresenciaCampo;

import lombok.Data;

@Data
public class ValorExtraidoModel implements Serializable {

	private static final long serialVersionUID = 3364820119975521840L;

	private String id;

	private String claveCampo;

	private String etiqueta;

	private String valorCrudo;

	private String valorNormalizado;

	private PresenciaCampo presencia;

	private BigDecimal confianza;

	private BigDecimal confianzaProveedor;

	private int evidenciaPagina;

	private String evidenciaRecuadro;

	private boolean corregidoManualmente;

	private String valorAnterior;

	private boolean anonimizado;
}
