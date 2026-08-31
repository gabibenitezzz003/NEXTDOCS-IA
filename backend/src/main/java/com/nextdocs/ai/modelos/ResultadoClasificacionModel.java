package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;

import lombok.Data;

@Data
public class ResultadoClasificacionModel implements Serializable {

	public static final String CODIGO_DESCONOCIDO = "DESCONOCIDO";

	private static final long serialVersionUID = 4419036628571900412L;

	private ProveedorDocumentalIa proveedor;

	private String modelo;

	private String codigoPropuesto;

	private BigDecimal confianza;

	private String motivo;

	private String nombreSugerido;

	private long tokensEntrada;

	private long tokensSalida;

	private long duracionMilisegundos;

	private List<CampoSugeridoModel> camposSugeridos = new ArrayList<>();

	public boolean esDesconocido() {
		return codigoPropuesto == null || codigoPropuesto.isBlank()
				|| CODIGO_DESCONOCIDO.equalsIgnoreCase(codigoPropuesto.trim());
	}
}
