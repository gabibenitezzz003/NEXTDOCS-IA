package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;

import com.nextdocs.ai.enumeraciones.PresenciaCampo;

import lombok.Data;

@Data
public class ValorCanonicoModel implements Serializable {

	private static final long serialVersionUID = 6120347762288015540L;

	private String claveCampo;

	private String valorCrudo;

	private String valorNormalizado;

	private PresenciaCampo presencia;

	private BigDecimal confianza;

	private BigDecimal confianzaProveedor;

	private int evidenciaPagina;

	private String evidenciaRecuadro;
}
