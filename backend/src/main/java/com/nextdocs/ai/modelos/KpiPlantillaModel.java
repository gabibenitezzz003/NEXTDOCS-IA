package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class KpiPlantillaModel implements Serializable {

	private static final long serialVersionUID = 1128034756690214483L;

	private String codigo;

	private String nombre;

	private long volumen;

	private long documentosConExcepciones;

	private String salud;

	private Map<String, Long> porEstado = new LinkedHashMap<>();

	private List<BarraKpiModel> barras = new ArrayList<>();
}
