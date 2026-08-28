package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;

import com.nextdocs.ai.enumeraciones.AccionPresupuestoCosto;

import lombok.Data;

@Data
public class PoliticaCostoReqModel implements Serializable {

	private static final long serialVersionUID = 9021145583312046618L;

	private boolean activo;

	private BigDecimal presupuestoMensual;

	private BigDecimal umbralAlerta;

	private AccionPresupuestoCosto accionAlExceder;

	private String moneda;
}
