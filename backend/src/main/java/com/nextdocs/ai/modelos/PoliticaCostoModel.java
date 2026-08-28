package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;

import com.nextdocs.ai.enumeraciones.AccionPresupuestoCosto;

import lombok.Data;

@Data
public class PoliticaCostoModel implements Serializable {

	private static final long serialVersionUID = 5519027741208836402L;

	private String id;

	private boolean activo;

	private BigDecimal presupuestoMensual;

	private BigDecimal umbralAlerta;

	private AccionPresupuestoCosto accionAlExceder;

	private String moneda;
}
