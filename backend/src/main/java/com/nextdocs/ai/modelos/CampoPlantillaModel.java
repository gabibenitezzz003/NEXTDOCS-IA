package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;

import com.nextdocs.ai.enumeraciones.SensibilidadCampo;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;

import lombok.Data;

@Data
public class CampoPlantillaModel implements Serializable {

	private static final long serialVersionUID = 5528140923677101284L;

	private String id;

	private String clave;

	private String etiqueta;

	private TipoDatoCampo tipoDato;

	private String alias;

	private String descripcion;

	private boolean requerido;

	private boolean extraer;

	private boolean validar;

	private boolean comparar;

	private boolean unico;

	private String expresionRegular;

	private String formatoFecha;

	private String catalogoReferencia;

	private BigDecimal umbralConfianza;

	private SensibilidadCampo sensibilidad;

	private int orden;
}
