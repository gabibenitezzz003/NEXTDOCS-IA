package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class KpiResumenModel implements Serializable {

	private static final long serialVersionUID = 5502914477803162238L;

	private RangoKpiModel rango;

	private List<IndicadorKpiModel> indicadores = new ArrayList<>();

	private Map<String, Long> porEstado = new LinkedHashMap<>();
}
