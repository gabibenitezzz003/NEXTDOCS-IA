package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.Data;

@Data
public class BarraKpiModel implements Serializable {

	private static final long serialVersionUID = 8814470255632190047L;

	private String clave;

	private String etiqueta;

	private BigDecimal valor;

	private BigDecimal porcentaje;

	private String semaforo;

	private String formula;

	public static BarraKpiModel de(String clave, String etiqueta, BigDecimal valor, String formula) {
		BarraKpiModel barra = new BarraKpiModel();
		barra.setClave(clave);
		barra.setEtiqueta(etiqueta);
		barra.setValor(valor);
		barra.setFormula(formula);
		barra.setPorcentaje(
				valor == null ? null : valor.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP));
		barra.setSemaforo(semaforoDe(valor));
		return barra;
	}

	private static String semaforoDe(BigDecimal valor) {
		if (valor == null) {
			return "SIN_DATOS";
		}
		if (valor.compareTo(new BigDecimal("0.85")) >= 0) {
			return "VERDE";
		}
		if (valor.compareTo(new BigDecimal("0.60")) >= 0) {
			return "AMBAR";
		}
		return "ROJO";
	}
}
