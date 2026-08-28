package com.nextdocs.ai.modelos;

import java.io.Serializable;

import lombok.Data;

@Data
public class CuentaServicioCreadaModel implements Serializable {

	private static final long serialVersionUID = 7395013049162557788L;

	private CuentaServicioModel cuenta;

	private String clave;

	private String advertencia;
}
