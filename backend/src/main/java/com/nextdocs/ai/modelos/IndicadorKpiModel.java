package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.Data;

@Data
public class IndicadorKpiModel implements Serializable {

	private static final long serialVersionUID = 3390561124478835502L;

	public static final String UNIDAD_CONTEO = "CONTEO";

	public static final String UNIDAD_PORCENTAJE = "PORCENTAJE";

	public static final String UNIDAD_HORAS = "HORAS";

	private String clave;

	private String etiqueta;

	private String unidad;

	private BigDecimal valor;

	private BigDecimal valorAnterior;

	private BigDecimal variacion;

	private String tendencia;

	private Long numerador;

	private Long denominador;

	private String formula;

	private String detalle;

	private boolean tienePoblacion;

	public static IndicadorKpiModel conteo(String clave, String etiqueta, long valor, Long anterior,
			String formula) {
		IndicadorKpiModel indicador = base(clave, etiqueta, UNIDAD_CONTEO, formula);
		indicador.setValor(BigDecimal.valueOf(valor));
		indicador.setValorAnterior(anterior == null ? null : BigDecimal.valueOf(anterior));
		indicador.calcularVariacion();
		return indicador;
	}

	public static IndicadorKpiModel porcentaje(String clave, String etiqueta, BigDecimal razon,
			BigDecimal razonAnterior, String formula, long numerador, long denominador) {
		IndicadorKpiModel indicador = base(clave, etiqueta, UNIDAD_PORCENTAJE, formula);
		indicador.setValor(razon == null ? null : razon.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP));
		indicador.setValorAnterior(razonAnterior == null ? null
				: razonAnterior.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP));
		indicador.setNumerador(numerador);
		indicador.setDenominador(denominador);
		indicador.calcularVariacion();
		return indicador;
	}

	public static IndicadorKpiModel duracion(String clave, String etiqueta, BigDecimal horas,
			BigDecimal horasAnterior, String formula) {
		IndicadorKpiModel indicador = base(clave, etiqueta, UNIDAD_HORAS, formula);
		indicador.setValor(horas);
		indicador.setValorAnterior(horasAnterior);
		indicador.calcularVariacion();
		return indicador;
	}

	private static IndicadorKpiModel base(String clave, String etiqueta, String unidad, String formula) {
		IndicadorKpiModel indicador = new IndicadorKpiModel();
		indicador.setClave(clave);
		indicador.setEtiqueta(etiqueta);
		indicador.setUnidad(unidad);
		indicador.setFormula(formula);
		return indicador;
	}

	private void calcularVariacion() {
		if (valor == null || valorAnterior == null || valorAnterior.compareTo(BigDecimal.ZERO) == 0) {
			tendencia = "SIN_COMPARACION";
			return;
		}
		variacion = valor.subtract(valorAnterior).multiply(new BigDecimal("100"))
				.divide(valorAnterior.abs(), 1, RoundingMode.HALF_UP);
		int signo = variacion.compareTo(BigDecimal.ZERO);
		tendencia = signo > 0 ? "SUBE" : signo < 0 ? "BAJA" : "ESTABLE";
	}
}
