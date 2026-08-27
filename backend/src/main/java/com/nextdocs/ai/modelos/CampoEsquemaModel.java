package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;

import com.nextdocs.ai.enumeraciones.TipoDatoCampo;

import lombok.Data;

@Data
public class CampoEsquemaModel implements Serializable {

	private static final long serialVersionUID = 8320477165098124331L;

	private String clave;

	private String etiqueta;

	private TipoDatoCampo tipoDato;

	private String descripcion;

	private String alias;

	private boolean requerido;

	private String formatoFecha;

	private String expresionRegular;

	private BigDecimal umbralConfianza;
}
